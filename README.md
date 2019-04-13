= XML Calabash TextDiff Step

This repisitory contains an extension step for
http://github.com/ndw/xmlcalabash1[XML Calabash]
to compare two text files. It uses the Java Diff Utils;
specifically, it has been tested against Jair Ogen's fork,
https://github.com/java-diff-utils/java-diff-utils/[Java Diff Utils].

This step should not care which version of Saxon you use, although it does depend on the Treebuilder API.

== Installation

TextDiff was tested on a Mageia Linux system but should be portable. To build,
./gradlew dist
should work; the dependencies are in Maven.

To deploy, you will need the TextDiff jar file in the class path for Calabash,
as well as the TextDiff jar file.

The step dc:TextDiff must be given two parameters, original-uri and
revised-uri; these are simple strings and must be the names of the two files you want to compare.
Despite the names, they are platform-specific file names, not URIs,
although that may change in the future. In particular, XML Catalogs are not used to resolve
them, and relative filenames will be looked for in the directory where the XML Calabash process is running.

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





