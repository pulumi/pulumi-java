# generative

These tests generate random package specs to test properties on.

The status of the generator is highly incomplete, a rough sketch.

The first property we are testing is that generated Java code always
compiles.

What could be interesting to test given working codegen is that the
generated packages do actually work in Java that is, they interact in
the execpted way with gRPC:

```
   def test_register_resource(lang)
      s = random_resource_schema()
      i = random_inputs(s.inputs_schema)

      sdk = codegen.generate_sdk(s)
      prog = pcl.generate_register_resource(pcl.generate_expr(lang, i), sdk)

      mock_engine = MockEngine()
      run(prog, mock_engine)

      assert mock_engine.register_resource_calls[0].grpc_inputs == to_grpc(i)
```
