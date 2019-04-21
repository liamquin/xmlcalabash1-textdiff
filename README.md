# XML Calabash TextDiff Extension Step

This repisitory contains an extension step for
[XML Calabash](https://github.com/ndw/xmlcalabash1/)
to compare two text files. It uses the Java Diff Utils;
specifically, it has been tested against Jair Ogen's fork,
[Java Diff Utils](https://github.com/java-diff-utils/java-diff-utils/).

This step should not care which version of Saxon you use, although it does depend on the Treebuilder API.

## Installation

TextDiff was tested on a Mageia Linux system but should be portable. To build,
you should have a copy of java-diff-utils-jycr in a parrall directory to this, and
build it first, and then
./gradlew dist
should work; all other dependencies are in Maven.

To deploy, you will need the TextDiff jar file in the class path for Calabash,
as well as the TextDiff jar file.

The step dc:TextDiff must be given two text files to compare, the original text and a revised version.

You can give the filename of the original in original-uri, or you can supply a
string whose contents are the entire text file in original-text; you can
instead supply a simple XML document containing one outer element (which should
be <dc:text> but this is not currently checked) whose text contents is the file
on the input port "original". The input source port is not read if either of
the original-uri or original-text attributes are present. It's an error to give
both attributes.

The revised file is given in the same way with revised-uri, revised-text or the
"revised" input port.

Despite the names, original-uri and revised-uri are platform-specific file
names, not URIs, although that may change in the future. In particular, XML
Catalogs are not used to resolve them, and relative filenames will be looked
for in the directory where the XML Calabash process is running.


Given the following files
````
original.txt:       revised.txt:
Simon               Simon
Andrew              Andrew
David               Nigel
Julie               Amanda
                    Julie
````
then the result consists of an XML-encoded tree:

````
<dc:textdiff
    xmlns:dc="http://www.delightfulcomputing.com/ns/"
    original-uri="original.txt" revised-uri="revised.txt">
  <dc:delta type="change">
    <dc:original position="2" size="1">
      <dc:chunk>David</dc:chunk>
    </dc:original>
    <dc:revised position="2" size="2">
      <dc:chunk>Nigel</dc:chunk>
      <dc:chunk>Amanda</dc:chunk>
    </dc:revised>
  </dc:delta>
</dc:textdiff>

````

Note that the Diff Utils number text lines starting at zero, not one,
so a change on the first line will have the position attribute set to zero.

There is an XSLT 1 stylesheet in lib/textdifftopatch.xsl that converts the XML result to minial Unix diff output, and there are a couple of sample single-step pipelines in the distribution.



