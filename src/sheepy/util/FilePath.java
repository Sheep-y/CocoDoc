package sheepy.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import sheepy.util.collection.Dictionary;

public class FilePath {

   private static FileWatcher fileWatcher;

   public static void watchFile ( Path path, Consumer< WatchEvent< Path >[] > callback ) {
      watchFile( Collections.singletonList( path ), callback );
   }

   /**
    * Watch given files for modifications, including modify and delete
    *
    * @param paths
    * @param callback
    */
   public static void watchFile ( List< Path > paths, Consumer< WatchEvent< Path >[] > callback ) {
      if ( paths == null || paths.isEmpty() ) return;
      try {
         synchronized ( FilePath.class ) {
            if ( fileWatcher == null ) fileWatcher = new FileWatcher();
         }

         for ( Path p : paths ) {
            p = p.toAbsolutePath();
            fileWatcher.list.get( p ).add( callback );
            if ( p.toFile().isFile() ) p = p.getParent();
            p.register( fileWatcher.watcher, ENTRY_MODIFY, ENTRY_DELETE );
         }
      } catch ( IOException ex ) {
         throw new RuntimeException( ex );
      }

      fileWatcher.startWatch();
   }

   /* An internal class with final fields to simplify sync issue, without initializing the fields in outer class */
   private static class FileWatcher implements Runnable {
      private final Map< Path, Set< Consumer< WatchEvent< Path >[] > > > list = new Dictionary.Set<>();
      private final WatchService watcher;
      private Thread thread;

      private FileWatcher() throws IOException {
         this.watcher = FileSystems.getDefault().newWatchService();
      }

      private void startWatch () {
         if ( thread != null ) return;
         assert( watcher != null );
         synchronized( watcher ) {
            thread = new Thread( this );
         }
         thread.setDaemon( true );
         thread.setPriority( Thread.NORM_PRIORITY-1 );
         thread.start();
      }

      public void run () {
         try {
            Map< Consumer< WatchEvent< Path >[] >, List< WatchEvent< Path > > > callList = new Dictionary.List<>();
            while ( ! Thread.currentThread().isInterrupted() ) {
               WatchKey key;
               key = watcher.take();
               for ( WatchEvent< ? > evt : key.pollEvents() ) {
                  if ( evt.kind() == OVERFLOW ) continue;
                  WatchEvent<Path> e = (WatchEvent<Path>) evt;
                  Path p = e.context().toAbsolutePath();
                  if ( list.containsKey( p ) ) {
                     for ( Consumer< WatchEvent< Path >[] > callback : list.get( p ) ) {
                        callList.get( callback ).add( e );
                     }
                  }
               }
               key.reset();

               for ( Map.Entry< Consumer< WatchEvent< Path >[] >, List< WatchEvent< Path > > > entry : callList.entrySet() ) {
                  try {
                     List< WatchEvent< Path > > list = entry.getValue();
                     if ( list.size() == 1 )
                        entry.getKey().accept( new WatchEvent[]{ list.get( 0 ) } );
                     else
                        entry.getKey().accept( list.toArray( new WatchEvent[ list.size() ] ) );
                  } catch ( RuntimeException ex ) {
                     ex.printStackTrace();
                  }
               }
            }
         } catch (InterruptedException ex) {
            synchronized ( watcher ) {
               thread = null;
            }
         }
      }

   }

}