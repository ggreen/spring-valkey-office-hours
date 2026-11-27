#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ] || ! [[ "$1" =~ ^[1-9][0-9]*$ ]]; then
  echo "Usage: $0 <instance-count>" >&2
  echo "Error: Instance count is required and must be a positive integer." >&2
  exit 1
fi

cf scale product-app -i "$1"