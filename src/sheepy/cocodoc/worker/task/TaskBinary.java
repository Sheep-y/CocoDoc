package sheepy.cocodoc.worker.task;

import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TaskBinary extends Task {

   @Override public Action getAction () { return Action.BINARY; }

   @Override protected Predicate<List<String>> validParam() { return null; }

   @Override protected void run () {
      log( Level.FINER, "Converting data to binary" );

      List<Charset> charsets = getParams().stream().map( Charset::forName ).collect( Collectors.toList() );
      getBlock().toBinary( charsets.isEmpty() ? null : charsets );

      log( Level.FINER, "Converted data to binary" );
   }
}