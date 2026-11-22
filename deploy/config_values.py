"""Leer solo metadatos de despliegue del JSON de Compose; nunca imprimir secretos."""
import ipaddress
import json
import re
import sys


def values(config):
    backend = config['services']['backend']
    env = backend['environment']
    for key in ('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET'):
        if not env.get(key) or 'REEMPLAZAR' in env[key]:
            raise ValueError(f'Completa {key} en el archivo privado de la VM.')
    if not env['DB_URL'].startswith('jdbc:mysql://'):
        raise ValueError('DB_URL debe ser una URL JDBC de MySQL.')
    port = backend['ports'][0]
    address = str(ipaddress.IPv4Address(port['host_ip']))
    if address == '0.0.0.0':
        raise ValueError('Usa la IP concreta de ZeroTier para publicar DOM2.')
    published = int(port['published'])
    if not 1024 <= published <= 65535:
        raise ValueError('El puerto publicado debe estar entre 1024 y 65535.')
    network = config['networks']['base_existente']['name']
    database = env.get('DOM2_DB_CONTAINER', 'ocasion_db_dom2')
    if not all(re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9_.-]*', name) for name in (network, database)):
        raise ValueError('Nombre de red o de contenedor MySQL inválido.')
    return address, str(published), network, database


if __name__ == '__main__':
    try:
        print('\n'.join(values(json.load(sys.stdin))))
    except json.JSONDecodeError:
        print('Docker Compose no devolvio una configuracion JSON valida. Revisa el error anterior de Docker.', file=sys.stderr)
        sys.exit(1)
    except (ValueError, KeyError, IndexError, TypeError) as ex:
        print(f'Configuración inválida: {ex}', file=sys.stderr)
        sys.exit(1)
