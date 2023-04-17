#!/usr/bin/env python

import logging
import sys
import time
import os

from translator import Translator
from djl_python.encode_decode import encode, decode
from djl_python import Input, Output

class HuggingFaceService:
    def __init__(self):
        self.initialized = False
        self.translator = None

    def initialize(self, properties: dict):
        device_id = int(properties.get("device_id", "-1"))
        # HF Acc handling
        kwargs = {}
        # https://huggingface.co/docs/accelerate/usage_guides/big_modeling#designing-a-device-map
        if "device_map" in properties:
            kwargs["device_map"] = properties.get("device_map")
            logging.info(f"Using device map {kwargs['device_map']}")
        if "load_in_8bit" in properties:
            if "device_map" not in kwargs:
                raise ValueError("device_map should set when load_in_8bit is set")
            kwargs["load_in_8bit"] = properties.get("load_in_8bit")
        if "low_cpu_mem_usage" in properties:
            kwargs["low_cpu_mem_usage"] = properties.get("low_cpu_mem_usage")

        dir_path = os.path.dirname(os.path.realpath(__file__))
        model_path = os.path.join(dir_path, '../raw-files/t5.1.1.lm100k.large')

        self.translator = Translator(model_path, **kwargs)
        self.initialized = True

    def translate(self, inputs):
        outputs = Output()
        if inputs.is_batch():
            batches = inputs.get_batches()
            raw_inputs = [batch.get_as_string("query") for batch in batches]
            # Assumes that each batch is using the same "expectedPrefix" and "isIncremental" settings.
            expected_prefix = '<pad>' + batches[0].get_as_string("expectedPrefix")
            is_incremental = batches[0].get_as_string("isIncremental") == "True"
            sql_outputs = self.translator.translate(expected_prefix, raw_inputs, is_incremental)
            for i, sql_output in enumerate(sql_outputs):
                outputs.add(sql_output, key="sql", batch_index=i)
        else:
            expected_prefix = '<pad>' + inputs.get_as_string("expectedPrefix")
            raw_input = inputs.get_as_string("query")
            is_incremental = inputs.get_as_string("isIncremental") == "True"
            sql_output = self.translator.translate(expected_prefix, [raw_input], is_incremental)[0]
            outputs.add(sql_output, key="sql")
        return outputs

_service = HuggingFaceService()

def handle(inputs: Input):
    if not _service.initialized:
        # stateful model
        before = time.perf_counter()
        _service.initialize(inputs.get_properties())
        after = time.perf_counter()
        logging.info(f'Model loaded in {str(after - before)} seconds')

    if inputs.is_empty():
        # initialization request
        return None

    return _service.translate(inputs)
