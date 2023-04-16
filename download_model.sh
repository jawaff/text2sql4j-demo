#!/bin/bash

mkdir raw-files
mkdir raw-files/t5.1.1.lm100k.large/

sudo apt-get install git-lfs
git lfs install

cd raw-files/t5.1.1.lm100k.large/
git clone git@hf.co:tscholak/3vnuv1vf .
cd ../../

# This is the t5-3b model, which is much larger at around 11gb and 10 billion nodes.
# mkdir raw-files/t5.3b/
# cd raw-files/t5.3b/
# git clone git@hf.co:tscholak/cxmefzzi .
# cd ../../
