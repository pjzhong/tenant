package org.example.benchmark.serde;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.example.serde.CollectionSerializer;
import org.example.serde.DefaultSerializersRegister;
import org.example.serde.Serdes;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class SerdeImpl {

  CodecObject object;

  Serdes codeSerde;
  ByteBuf buf;

  @Setup
  public void prepare() {
    object = new CodecObject();

    codeSerde = new Serdes();
    new DefaultSerializersRegister().register(codeSerde);
    codeSerde.registerSerializer(CodecObject.class, new CodecObjectSerde());
    codeSerde.registerSerializer(List.class, new CollectionSerializer());

    buf = Unpooled.buffer(1024);
  }

  @Setup(Level.Iteration)
  public void refreshObj() throws InterruptedException {
    object = new CodecObject();
  }


  @Benchmark
  @Warmup
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void codecSerdeTest(Blackhole bh) {
    buf.clear();
    codeSerde.serialize(buf, object);
    CodecObject object2 = codeSerde.deserialize(buf);

    if (!object2.equals(object)) {
      throw new RuntimeException();
    }
    bh.consume(object2);
  }


}
