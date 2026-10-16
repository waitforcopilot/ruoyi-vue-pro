"""Private-state, real-login client for local isolated HRM runtime regressions."""
import json
import os
from pathlib import Path
import urllib.error
import urllib.parse
import urllib.request


class RuntimeClient:
    def __init__(self, state_file):
        self.state_file = Path(state_file).resolve()
        repository = Path(__file__).resolve().parents[2]
        if repository in self.state_file.parents:
            raise ValueError('Credentials must stay outside the repository')
        self.state = json.loads(self.state_file.read_text())
        self.base = self.state.get('baseUrl', 'http://127.0.0.1:48080/admin-api').rstrip('/')
        if urllib.parse.urlparse(self.base).hostname not in ('127.0.0.1', 'localhost'):
            raise ValueError('Only local isolated runtime environments are supported')
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        self.token = ''
        self.token = self.login('admin', 'adminPassword')
        if not all(row['code'].startswith('ACCEPT_') and '隔离' in row['name']
                   for row in self.ok('/hrm/payroll/entity/list')):
            raise ValueError('Only synthetic fixture subjects are allowed')

    def request(self, path, data=None, token=None, method=None):
        request = urllib.request.Request(
            self.base + path,
            data=None if data is None else json.dumps(data).encode(),
            headers={'tenant-id': '1', 'Content-Type': 'application/json',
                     'Authorization': 'Bearer ' + (self.token if token is None else token)},
            method=method,
        )
        try:
            with self.opener.open(request, timeout=60) as response:
                body = response.read()
        except urllib.error.HTTPError as error:
            body = error.read()
        return json.loads(body) if body[:1] in (b'{', b'[') else body

    def ok(self, path, data=None, token=None, method=None):
        result = self.request(path, data, token, method)
        if not isinstance(result, dict) or result.get('code') != 0:
            raise AssertionError(f'{path.split("?")[0]}: {result}')
        return result['data']

    def query(self, path, params, token=None):
        return self.ok(path + '?' + urllib.parse.urlencode(params), {}, token)

    def login(self, username, password_key):
        return self.ok('/system/auth/login', {'username': username, 'password': self.state[password_key]}, token='')['accessToken']

    def save(self):
        fd = os.open(self.state_file, os.O_WRONLY | os.O_TRUNC, 0o600)
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, 'w') as output:
            json.dump(self.state, output)
