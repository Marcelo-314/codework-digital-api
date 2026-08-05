#!/bin/sh
set -eu

unsupported_url() {
    echo "Render database connection URL has an unsupported format." >&2
    exit 1
}

if [ "$#" -eq 0 ]; then
    echo "No command was provided." >&2
    exit 1
fi

if [ -z "${SPRING_DATASOURCE_URL:-}" ] && [ -n "${RENDER_DATABASE_URL:-}" ]; then
    case "$RENDER_DATABASE_URL" in
        postgresql://*@*/*)
            database_authority_path=${RENDER_DATABASE_URL#postgresql://}
            database_userinfo=${database_authority_path%%@*}
            database_host_path=${database_authority_path#*@}
            database_host=${database_host_path%%/*}
            database_path=${database_host_path#*/}
            if [ -z "$database_userinfo" ] || [ -z "$database_host" ] || [ -z "$database_path" ]; then
                unsupported_url
            fi
            export SPRING_DATASOURCE_URL="jdbc:postgresql://$database_host_path"
            ;;
        *)
            unsupported_url
            ;;
    esac
fi

exec "$@"
