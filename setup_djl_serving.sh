#!/bin/bash

# This setup comes from instructions listed here:
# https://github.com/deepjavalibrary/djl-demo/tree/master/djl-serving/python-mode
git clone https://github.com/deepjavalibrary/djl-serving -b v0.21.0
cd djl-serving/engines/python/setup
pip install -U .
cd ../../../../
