package sheepy.cocodoc;

public class CocoParseError extends RuntimeException {

   public CocoParseError() {
   }

   public CocoParseError(String message) {
      super(message);
   }

   public CocoParseError(String message, Throwable cause) {
      super(message, cause);
   }

   public CocoParseError(Throwable cause) {
      super(cause);
   }

   public CocoParseError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }
   
}
