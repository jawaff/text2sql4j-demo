1. TensorRT Engine:

The TensorRT engine seems to increase performance of a T5 model by several times, but currently doesn't support a beam search.
One requirement is that the model is converted to the ONNYX format.

https://forums.developer.nvidia.com/t/using-beam-search-with-the-tensorrt-compiled-t5-model/210498

2. Optimum:

Optimum says it supports text generation, but only some models are compatible. Examples are limited and I'm not
sure where they are with all of that.

3. ONNYX:

It does seem possible to get beam search working with ONNYX models, but it needs to be TorchScript compatible and
correctly do the tracing for all of the autoregressive features. Work is being done for Huggingface to adequately
support ONNYX generate and beam search.

https://github.com/huggingface/transformers/tree/main/examples/research_projects/onnx/summarization
https://discuss.huggingface.co/t/support-for-exporting-generate-function-to-onnx/21501/5
https://github.com/huggingface/optimum/issues/526