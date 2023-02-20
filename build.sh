#!/bin/bash

set -euo pipefail

clj -M -e "(compile 'lazr.core)" && clj -M:uberjar
