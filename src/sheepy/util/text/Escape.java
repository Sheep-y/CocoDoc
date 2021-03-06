package sheepy.util.text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Escape {

   private static final Pattern csv_quote_regx = Pattern.compile( ".*[\",\n].*" );
   public static String csv( String txt ) {
      if ( txt.isEmpty() || ! csv_quote_regx.matcher( txt ).matches() ) return txt;
      return '"' + txt.replaceAll( "\"", "\"\"" ) + '"';
   }

   public static String xml ( CharSequence text ) {
      if ( text == null ) return null;
      return text.toString().replaceAll("&", "&amp;").replaceAll("/", "&#34;").replaceAll("'", "&#39;").replaceAll("\"", "&#47;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
   }

   public static String url ( CharSequence text ) {
      if ( text == null ) return null;
      try {
         return URLEncoder.encode( text.toString(), "UTF-8" ).replaceAll( "\\+", "%20" );
      } catch ( UnsupportedEncodingException ex ) {
         throw new RuntimeException( ex );
      }
   }

   public static String javascript ( CharSequence text ) {
      if ( text == null ) return null;
      return text.toString().replaceAll( "[\"'`\n\r]", "\\\\$0" );
   }


   /**
    * Convert binary data byte by byte in UTF-8 and then url encode.
    *
    * @param data Binary data
    * @return url encoded binary data. Can be used for decodeURIComponent( atob( data ) )
    *
   private static final String[] chr_perc = new String[ 256 ];
   public static CharSequence url ( byte[] data ) {
      StringBuilder result = new StringBuilder( data.length * 2 );
      for ( byte i : data ) {
         // Non-reserved characters
         if ( isUrlUnreserved( (char) i ) ) {
            result.append( (char) i );
            continue;
         }
         int j = i & 0x00ff;
         // Reserved characters, need encode... from mapping!
         String code = chr_perc[ j ];
         if ( code == null ) {
            synchronized ( chr_perc ) {
               if ( chr_perc[ j ] == null ) {
                  for ( int k = 0 ; k <= 255 ; k++ ) {
                     if ( k < 0x80 )      chr_perc[ k ] = "%"    + Integer.toString( k, 16 );
                     else if ( k < 0xC0 ) chr_perc[ k ] = "%C2%" + Integer.toString( k, 16 );
                     else                 chr_perc[ k ] = "%C3%" + Integer.toString( k - 0x40, 16 );
                  }
               }
            }
            code = chr_perc[ j ];
         }
         result.append( code );
      }
      return result;
   }

   public static boolean isUrlUnreserved( char chr ) {
      return ( chr >= 65 && chr <= 90 ) || ( chr >= 97 && chr <= 122 ) || ( chr >= 48 && chr <= 57 ) || // A-Z, a-z, 0-9
        ( chr == 45 || chr == 46 || chr == 95 || chr == 126 ); // -._~
   }
   */


   private static final Pattern numeric_entity_regx = Pattern.compile( "&(?:#x([a-fA-F0-9]{1,5})|#(\\d{1,6})|([a-zA-Z]{2,8}))(;)?" );
   private static final Map< String, Character > xhtml_entity_map = new HashMap<>( 253, 1.0f );
   // 253 entities, size 2 to 8; from http://www.w3.org/TR/xhtml11/DTD/xhtml11-flat.dtd
   private static final String xhtml_entity = "nbsp:160,iexcl:161,cent:162,pound:163,curren:164,yen:165,brvbar:166,sect:167,uml:168,copy:169,ordf:170,laquo:171,not:172,shy:173,reg:174,macr:175,deg:176,plusmn:177,sup2:178,sup3:179,acute:180,micro:181,para:182,middot:183,cedil:184,sup1:185,ordm:186,raquo:187,frac14:188,frac12:189,frac34:190,iquest:191,Agrave:192,Aacute:193,Acirc:194,Atilde:195,Auml:196,Aring:197,AElig:198,Ccedil:199,Egrave:200,Eacute:201,Ecirc:202,Euml:203,Igrave:204,Iacute:205,Icirc:206,Iuml:207,ETH:208,Ntilde:209,Ograve:210,Oacute:211,Ocirc:212,Otilde:213,Ouml:214,times:215,Oslash:216,Ugrave:217,Uacute:218,Ucirc:219,Uuml:220,Yacute:221,THORN:222,szlig:223,agrave:224,aacute:225,acirc:226,atilde:227,auml:228,aring:229,aelig:230,ccedil:231,egrave:232,eacute:233,ecirc:234,euml:235,igrave:236,iacute:237,icirc:238,iuml:239,eth:240,ntilde:241,ograve:242,oacute:243,ocirc:244,otilde:245,ouml:246,divide:247,oslash:248,ugrave:249,uacute:250,ucirc:251,uuml:252,yacute:253,thorn:254,yuml:255,fnof:402,Alpha:913,Beta:914,Gamma:915,Delta:916,Epsilon:917,Zeta:918,Eta:919,Theta:920,Iota:921,Kappa:922,Lambda:923,Mu:924,Nu:925,Xi:926,Omicron:927,Pi:928,Rho:929,Sigma:931,Tau:932,Upsilon:933,Phi:934,Chi:935,Psi:936,Omega:937,alpha:945,beta:946,gamma:947,delta:948,"
      + "epsilon:949,zeta:950,eta:951,theta:952,iota:953,kappa:954,lambda:955,mu:956,nu:957,xi:958,omicron:959,pi:960,rho:961,sigmaf:962,sigma:963,tau:964,upsilon:965,phi:966,chi:967,psi:968,omega:969,thetasym:977,upsih:978,piv:982,bull:8226,hellip:8230,prime:8242,Prime:8243,oline:8254,frasl:8260,weierp:8472,image:8465,real:8476,trade:8482,alefsym:8501,larr:8592,uarr:8593,rarr:8594,darr:8595,harr:8596,crarr:8629,lArr:8656,uArr:8657,rArr:8658,dArr:8659,hArr:8660,forall:8704,part:8706,exist:8707,empty:8709,nabla:8711,isin:8712,notin:8713,ni:8715,prod:8719,sum:8721,minus:8722,lowast:8727,radic:8730,prop:8733,infin:8734,ang:8736,and:8743,or:8744,cap:8745,cup:8746,int:8747,there4:8756,sim:8764,cong:8773,asymp:8776,ne:8800,equiv:8801,le:8804,ge:8805,sub:8834,sup:8835,nsub:8836,sube:8838,supe:8839,oplus:8853,otimes:8855,perp:8869,sdot:8901,lceil:8968,rceil:8969,lfloor:8970,rfloor:8971,lang:9001,rang:9002,loz:9674,spades:9824,clubs:9827,hearts:9829,diams:9830,quot:34,amp:38,lt:60,gt:62,apos:39,OElig:338,oelig:339,Scaron:352,scaron:353,Yuml:376,circ:710,tilde:732,ensp:8194,emsp:8195,thinsp:8201,zwnj:8204,zwj:8205,lrm:8206,rlm:8207,ndash:8211,mdash:8212,lsquo:8216,rsquo:8217,sbquo:8218,ldquo:8220,rdquo:8221,bdquo:8222,dagger:8224,Dagger:8225,permil:8240,lsaquo:8249,rsaquo:8250,euro:8364";

   public static CharSequence unHtml( CharSequence input ) { return unHtml( input, false, CodingErrorAction.IGNORE ); }
   public static CharSequence unHtml( CharSequence input, boolean requireSemiColon, CodingErrorAction onerror  ) {
      if ( input == null || input.length() <= 0 ) return input;
      String test = input.toString();
      if ( test.indexOf( '&' ) < 0 ) return input;
      else if ( test.startsWith( "<![CDATA[" ) && test.endsWith( "]]>" ) ) return test.substring( 9, test.length()-3 );

      final StringBuilder result = new StringBuilder( input.length() );
      final Matcher m = numeric_entity_regx.matcher( input );
      for ( int pos = 0, len = input.length(), codepoint ; pos < len ; ) {
         if ( ! m.find( pos ) )
            return result.append( input, pos, len ); // We are done
         if ( m.start() != pos )
            result.append( input, pos, m.start() ); // Append text before entity to result

         pos = m.end();
         if ( requireSemiColon && m.group( 4 ) == null ) codepoint = 0;
         else if ( m.group( 1 ) != null ) codepoint = Integer.parseInt( m.group( 1 ), 16 ); // Hex
         else if ( m.group( 2 ) != null ) codepoint = Integer.parseInt( m.group( 2 ) );     // Dec
         else {                                                                             // Text
            if ( xhtml_entity_map.isEmpty() ) synchronized ( xhtml_entity_map ) { if ( xhtml_entity_map.isEmpty() ) {
               for ( String e : xhtml_entity.split( "," ) ) { // Populate map with character mappings
                  int colon = e.indexOf( ':' );
                  xhtml_entity_map.put( e.substring( 0, colon ), (char) Short.parseShort( e.substring( colon+1 ) ) );
               }
            } }
            String entity = m.group( 3 );
            codepoint = 0;
            do { // Try less and less character until we have an entity match or there would be no match
               Character chr = xhtml_entity_map.get( entity );
               if ( chr != null ) codepoint = chr;
               else entity = entity.substring( 0, entity.length() - 1 );
            } while ( codepoint == 0 && m.group( 4 ) == null && ! requireSemiColon && entity.length() >= 2 );
            if ( codepoint != 0 ) pos -= m.group( 3 ).length() - entity.length();
         }

         try {
            if ( codepoint == 0 || ( m.group( 3 ) != null && m.group( 4 ) == null && codepoint > 255 ) )
               throw new IllegalArgumentException( "Invalid entity: " + m.group( 0 ) );
            else
               result.append( Character.toChars( codepoint ) );
         } catch ( IllegalArgumentException ex ) { // Invalid Codepoint
            if ( onerror == CodingErrorAction.REPORT ) throw ex;
            result.append( m.group( 0 ) );
         }
      }
      return result;
   }
}