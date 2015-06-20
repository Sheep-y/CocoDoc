package sheepy.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.logging.Level;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Net {

   /**
    * Call this function to trust all certificates in future SSL connections.
    */
   public static void trustAllSSL () {
      synchronized ( TrustEveryoneManager.class ) {
         if ( trustedAll ) return;
         try {
             SSLContext sc = SSLContext.getInstance( "SSL" );
             sc.init( null, new TrustManager[]{ new TrustEveryoneManager() }, new SecureRandom() );
             HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory() );
             trustedAll = true;
         } catch ( NoSuchAlgorithmException | KeyManagementException ex ) {
            throw new RuntimeException( ex );
         }
      }
   }

   private static boolean trustedAll = false;
   private static class TrustEveryoneManager implements X509TrustManager {
       @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
       @Override public void checkClientTrusted( X509Certificate[] certs, String authType ) { }
       @Override public void checkServerTrusted( X509Certificate[] certs, String authType ) { }
   }

   /**
    * Returns a default console object.
    * log, debug, and info goes to System.out, warn and error goes to System.err.
    *
    * Example:
    * (Nashorn) scriptEngine.put( "console", Net.defaultConsole() );
    * (WebView) ((netscape.javascript.JSObject)webEngine.executeScript("window")).setMember( "console", Net.defaultConsole() );
    *
    * @return A shared console object.
    */
   public static Console defaultConsole() {
      synchronized ( Console.class ) {
         if ( defaultConsole != null ) return defaultConsole;
         return defaultConsole = ( level, args ) -> {
            if ( Level.SEVERE.intValue() < level.intValue() )
               System.out.println( Objects.toString( args ) );
            else
               System.err.println( Objects.toString( args ) );
         };
      }
   }

   /**
    * Console functional interface
    */
   public static interface Console {
      public default void group() {}
      public default void groupCollapsed() {}
      public default void groupEnd() {}
      public default void trace() { new Exception("Stack trace").printStackTrace(); }
      public default void log  ( Object args ) { handle( Level.INFO, args ); }
      public default void debug( Object args ) { handle( Level.FINE, args ); }
      public default void info ( Object args ) { handle( Level.CONFIG, args ); }
      public default void warn ( Object args ) { handle( Level.WARNING, args ); }
      public default void error( Object args ) { handle( Level.SEVERE, args ); }
      public void handle( Level level, Object args );
   }
   private static Console defaultConsole;

}