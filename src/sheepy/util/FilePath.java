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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import sheepy.util.collection.Dictionary;

public class FilePath {

   private static FileWatcher fileWatcher;

   /**
    * Watch given file for modifications (modify and delete).
    * For best performance, use the same callback whenever possible (instead of a new lambda every time).
    *
    * @param path File to watch
    * @param callback A Runnable that can be executed to cancel the watch.
    */
   public static Runnable watchFile ( Path path, Consumer< Path[] > callback ) {
      if ( path == null ) return () -> {};
      try {
         synchronized ( FileWatcher.class ) {
            if ( fileWatcher == null ) fileWatcher = new FileWatcher();
         }

         Path p = path.toAbsolutePath().normalize();
         Path folder = p.toFile().isFile() ? p.getParent() : p;
         WatchKey key = folder.register( fileWatcher.watcher, ENTRY_MODIFY, ENTRY_DELETE );
         FileWatchEntry entry = new FileWatchEntry( key, p, callback );
         fileWatcher.map.get( key ).add( entry );

         fileWatcher.startWatch();
         return () -> {
            synchronized ( fileWatcher.map ) {
               Set<FileWatchEntry> list = fileWatcher.map.get( key );
               list.remove( entry );
               if ( list.isEmpty() ) {
                  key.cancel();
                  fileWatcher.map.remove( key );
               }
            }
         };
      } catch ( IOException ex ) {
         throw new RuntimeException( ex );
      }
   }

   private static class FileWatchEntry {
      final WatchKey key;
      final Path file;
      final Path name;
      final Consumer< Path[] > callback;

      public FileWatchEntry( WatchKey key, Path file,Consumer< Path[] > callback) {
         this.key = key;
         this.callback = callback;
         this.file = file;
         name = file.getFileName();
      }

      @Override public int hashCode() {
         int hash = 7;
         hash = 97 * hash + Objects.hashCode( this.key );
         hash = 97 * hash + Objects.hashCode( this.file );
         hash = 97 * hash + Objects.hashCode( this.callback );
         return hash;
      }

      @Override public boolean equals(Object obj) {
         if ( obj == null || getClass() != obj.getClass() ) return false;
         final FileWatchEntry other = (FileWatchEntry) obj;
         if ( ! Objects.equals( this.key, other.key ) ) return false;
         if ( ! Objects.equals( this.file, other.file ) ) return false;
         if ( ! Objects.equals( this.callback, other.callback ) ) return false;
         return true;
      }
   }

   /* An internal class with final fields to simplify sync issue, without initializing the fields in outer class */
   private static class FileWatcher implements Runnable {
      private final Map< WatchKey, Set<FileWatchEntry> > map = new Dictionary.Set<>();
      private final WatchService watcher;
      private Thread thread;

      private FileWatcher() throws IOException {
         watcher = FileSystems.getDefault().newWatchService();
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
         Map< Consumer< Path[] >, List< Path > > callList = new Dictionary.List<>();
         try {
            while ( ! Thread.currentThread().isInterrupted() ) {
               WatchKey key = watcher.take();
               do {
                  if ( ! key.isValid() ) continue;
                  // Find relevant callback and file from each event, using WatchKey
                  synchronized ( map ) {
                     Set< FileWatchEntry > list = map.get( key );
                     for ( WatchEvent< ? > evt : key.pollEvents() ) {
                        if ( evt.kind() == OVERFLOW ) continue;
                        Path subject = ( (Path) evt.context() ).getFileName();
                        list.stream().filter( e -> subject.equals( e.name ) ).forEach( e -> {
                           callList.get( e.callback ).add( e.file );
                        } );
                     }
                  }
                  key.reset();
               } while ( null != ( key = watcher.poll( 100, TimeUnit.MILLISECONDS ) ) );

               for ( Map.Entry< Consumer< Path[] >, List< Path > > entry : callList.entrySet() ) {
                  try {
                     List< Path > list = entry.getValue();
                     if ( list.isEmpty() ) continue;
                     if ( list.size() == 1 )
                        entry.getKey().accept( new Path[]{ list.get( 0 ) } );
                     else
                        entry.getKey().accept( list.toArray( new Path[ list.size() ] ) );
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