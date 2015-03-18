package sheepy.cocodoc.worker.error;

public class CocoRunError extends RuntimeException {

   public CocoRunError() {
   }

   public CocoRunError(String message) {
      super(message);
   }

   public CocoRunError(String message, Throwable cause) {
      super(message, cause);
   }

   public CocoRunError(Throwable cause) {
      super(cause);
   }

   public CocoRunError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }
   
}
