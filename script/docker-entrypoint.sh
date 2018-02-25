#!/bin/bash
set -e

# usage: file_env VAR [DEFAULT]
#    ie: file_env 'XYZ_DB_PASSWORD' 'example'
# (will allow for "$XYZ_DB_PASSWORD_FILE" to fill in the value of
#  "$XYZ_DB_PASSWORD" from a file, especially for Docker's secrets feature)
file_env() {
  local var="$1"
  local fileVar="${var}_FILE"
  local def="${2:-}"
  if [ "${!var:-}" ] && [ "${!fileVar:-}" ]; then
    echo >&2 "error: both $var and $fileVar are set (but are exclusive)"
    exit 1
  fi
  local val="$def"
  if [ "${!var:-}" ]; then
    val="${!var}"
  elif [ "${!fileVar:-}" ]; then
    val="$(< "${!fileVar}")"
  fi
  export "$var"="$val"
  unset "$fileVar"
}

# set env
echo "Setting env"
file_env 'ENV_PHASE'
if [ ! "$ENV_PHASE" ]
then
	ENV_PHASE="prd"
fi

# create properties and setting env phase
echo "Create application.properties and copy application-$ENV_PHASE.propertise"
if [ ! -f /app/config/application.properties ]; then
	echo "spring.profiles.active=$ENV_PHASE" > /app/config/application.properties
fi

if [ ! -f /app/config/application-$ENV_PHASE.properties ]; then
	cp -f /application-$ENV_PHASE.properties /app/config/application-$ENV_PHASE.properties
fi

exec "$@"
