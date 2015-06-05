CocoDoc: HTML App builder
=========================

CocoDoc is a desktop utility program designed to build single file web documents and web applications from multiple files.

Java 1.8.0u40 or upper must be installed.

<!--

For Developers
--------------

To build: Extract the jar (or exe), then use Ant to run "make" task of build.xml.

Documentation in doc folder.
Source code in src folder.

This document is in GitHub markdown format.

-->

Major features:

  1. Hierarchically merge files and apply filters such as variable filling or converting to base64.
  2. Replace tag content or property with data, like replacing relative img src with base64 data uri.
  3. Parse HTML to generate Table of Content, Index, and/or Glossary.
  4. Minify HTML, CSS, and JS in one go as part of the build process.
  5. Parallel processing architecture that make the best use of multi-core processors.
  6. A GUI that shows instant progress and let you drill down the process log.

License: <a href='http://www.gnu.org/licenses/lgpl.html'>Lesser GPL v3</a>

Quick Start
-----------

CocoDoc does not need to be installed - just download, launch, and point it to the build file.
Windows users should download the exe file, Linux and Mac users may download the jar file.

The build file is assumed to be a text file containing commands, that the program will follows to build your project.

### I/O ###

The most basic commands are "include file" and "output file":

<pre><code>&lt;!DOCTYPE html&gt;&lt;srcipt&gt;
<b>&lt;?coco "js/jquery.min.js" ?&gt;</b>
&lt;/script&gt;
<b>&lt;?coco-output "myapp.html" ?&gt;</b></code></pre>

This code will includes content from "js/jquery.min.js" (relative to current file) into the &lt;srcipt&gt; tag, and export everything to "myapp.html" (again relative to current file).


### Hierarchy ###

You can also split the code into many small files.  Like a real program.

<dl>
   <dt>master.html</dt>
   <dd><pre><code>&lt;!DOCTYPE html&gt;&lt;srcipt&gt;<b>&lt;?coco "js/jquery.min.js" ?&gt;</b>&lt;/script&gt;
&lt;body&gt;<b>&lt;?coco "body.html" coco ?&gt;</b>
<b>&lt;?coco-output "myapp.html" ?&gt;</b></code></pre></dd>
   <dt>body.html</dt>
   <dd><pre><code><b>&lt;?coco-start trim(line) ?&gt;</b>
   &lt;h1&gt; Part 1 &lt;/h1&gt;
   <b>&lt;?coco "part1.html" ?&gt;</b>
   &lt;h1&gt; Part 2 &lt;/h1&gt;
   <b>&lt;?coco "part2.html" ?&gt;</b>
<b>&lt;?coco-end trim(line) ?&gt;</b></code></pre></dd>
</dl>

In the above example, master.html includes both <code>jquery.min.js</code> and <code>body.html</code>, the later containing the <code>coco</code> <i>task</i> which means it will run contained coco directives.
body.html, in turn, includes two other files by declaring a block that will be line-trimmed (Task <code>trim(line)</code>).


### Variables / Replace ###

Defining and using variable is simple:

<pre><code><b>&lt;?coco define( varname, value ) ?&gt;</b>
<b>&lt;?coco var( varname ) ?&gt;</b></code></pre>

Replacing content is equally simple, and supports regular expression:

<code><b>&lt;?coco replace( "(\d+).(\d+)", "$1,$2" ) ?&gt;</b></code>

The above code replace all floating point numbers to use comma as decimal point.
(This pattern is merely an example, not intended for real world application.)

### Data URI ###

Another simple but useful command is the datauri shortcut:

<code>&lt;img src='img/cocodoc.png' /&gt;<b>&lt;?coco-datauri "img/cocodoc.png" position(src of img before this) ?&gt;</b></code>

or in case of CSS:

<pre><code>background-image: url("<b>&lt;?coco-datauri "img/bkgd.png" delete(the line after this) ?&gt;</b>")
background-image: url("img/bkgd.png")</code></pre>

Both code will read the image file (relative to current file), convert it to a data uri, and replace the original, externally linked image.
This can be used to create a master pages that works both in multiple document mode (e.g. for easy development) and can be compiled to generate a single deployable.

Please note that the file need to be well formed XML for the position / delete task to function correctly.
If you do not use delete, position, or html, then it is not required.


### HTML Structure ###

The <code>html</code> task will parse content as XML for XHTML headers, index terms, and glossary terms.

<pre><code><b>&lt;?coco html(toc)?&gt;</b> &lt;!-- Output Table of Content --&gt;
<b>&lt;?coco-start html ?&gt;</b> &lt;!-- Parse HTML --&gt;
<b>&lt;h1</b> id='h1'&gt; Header <b>&lt;/h1&gt;</b>
<b>&lt;h2</b> id='a' class='a'&gt; A <b>&lt;/h2&gt;</b>
   &lt;div <b>data-coco-index="Coco"</b>&gt; An Index term. &lt;/div&gt;
   &lt;div <b>data-coco-glossary="Doc"</b>&gt; A Glossary term. &lt;/div&gt;
<b>&lt;h2</b> id='b' class='b'&gt; B <b>&lt;/h2&gt;</b>
   &lt;div <b>data-coco-index="Coco"</b>&gt; Another Index term. &lt;/div&gt;
   &lt;div <b>data-coco-glossary="Doc"</b>&gt; Another Glossary term. &lt;/div&gt;
&lt;?coco-end?&gt;
&lt;div id="index"&gt; <b>&lt;?coco html(index)?&gt;</b> &lt;/div&gt; &lt;!-- Output Index --&gt;
&lt;div id="glossary"&gt; <b>&lt;?coco html(glossary)?&gt;</b> &lt;/div&gt; &lt;!-- Output Glossary --&gt;</code></pre>

The above code will produce this:

<pre><code>&lt;ol class='h0'&gt;
&lt;li class='h1'&gt;&lt;a href="#h1"&gt; Header &lt;/a&gt;
&lt;ol class='h1'&gt;
   &lt;li class='h2'&gt;&lt;a href="#a" class='a'&gt; A &lt;/a&gt;&lt;/li&gt;
   &lt;li class='h2'&gt;&lt;a href="#b" class='b'&gt; B &lt;/a&gt;&lt;/li&gt;
&lt;/ol&gt; &lt;/li&gt;
&lt;/ol&gt;
&lt;!-- Original content goes here --&gt;
&lt;div id="index"&gt; &lt;dl&gt;
   &lt;dt&gt;Coco&lt;/dt&gt;
   &lt;dd&gt;&lt;a href="#a"&gt;1.1. A&lt;/a&gt;&lt;/dd&gt;
   &lt;dd&gt;&lt;a href="#b"&gt;1.2. B&lt;/a&gt;
&lt;/dd&gt;&lt;/dl&gt; &lt;/div&gt;
&lt;div id="glossary"&gt; &lt;dl&gt;
   &lt;dt&gt;Doc&lt;/dt&gt;
   &lt;dd&gt;&lt;div&gt; A Glossary term. &lt;/div&gt;&lt;/dd&gt;
   &lt;dd&gt;&lt;div&gt; Another Glossary term. &lt;/div&gt;&lt;/dd&gt;
&lt;/dl&gt; &lt;/div&gt;
</code></pre>

### Minify ###

CocoDoc has built-in [UglifyJS2](https://github.com/mishoo/UglifyJS2/releases) and [UglifyCSS](https://github.com/fmarcia/UglifyCSS).

Minify inline data and protect with cdata:

<pre>&lt;?coco-start <b>trim( html, oneline )</b> ?&gt;&lt;!DOCTYPE html&gt;
   &lt;style &gt; &lt;?coco "style.css" <b>css(minify) cdata(css)</b> ?&gt; &lt;/style&gt;
   &lt;script&gt; &lt;?coco "script.js" <b>js(minify)  cdata(js) </b> ?&gt; &lt;/script&gt;
&lt;body&gt;&lt;!-- HTML comments will be removed by trim( html ) --&gt;&lt;/body&gt;
&lt;?coco-end?&gt;</pre>

Minify external data:

<pre>&lt;?coco-start?&gt;&lt;?coco "style.css" uglifycss ?&gt;&lt;?coco-output "style.min.css" ?&gt;&lt;?coco-end?&gt;
&lt;?coco-start?&gt;&lt;?coco "script.js" uglifyjs  ?&gt;&lt;?coco-output "script.min.js" ?&gt;&lt;?coco-end?&gt;</pre>

### Auto-run ###

Passing file names as parameters will cause CocoDoc to build those files and, when finished, starts auto-close countdown.

If there are no file parameters, CocoDoc will look for the file <code>build.cocodoc.conf</code> to auto-run and auto-close.