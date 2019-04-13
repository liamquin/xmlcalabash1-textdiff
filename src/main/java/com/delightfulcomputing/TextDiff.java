package com.delightfulcomputing.xproc;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.XProcURIResolver;
import com.xmlcalabash.util.TreeWriter;


import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import java.util.*;
// import java.util.List;
// import java.util.LinkedList;
// import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.xml.transform.sax.SAXSource;

import difflib.Delta;
import difflib.Chunk;
import difflib.Patch;
import difflib.DiffUtils;


/**
 * Created by slave on 2019-03-30
 */

@XMLCalabash(
    name = "dc:textdiff",
    type = "{http://www.delightfulcomputing.com/ns/}textdiff"
)

public class TextDiff extends DefaultStep {
    public static final QName _text_file1 = new QName("", "text-file1");
    public static final QName _text_file2 = new QName("", "text-file2");


    // private static final String library_xpl = "ttps://www.delightfulcomputing.com/xproc/xpl/textdiff.xpl";
    // private static final String library_url = "/com/delightfulcomputing/extensions/textdiff/library.xpl";

    /* Attributes */
    private static final QName _original_uri = new QName("", "original-uri");
    private static final QName _revised_uri = new QName("", "revised-uri");

    private ReadablePipe source = null;
    private WritablePipe result = null;

    public TextDiff(XProcRuntime runtime, XAtomicStep step) {
        super(runtime, step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    private static String deltaType(Delta<String> delta) {
	if (delta == null) {
	    return "null";
	}

	switch (delta.getType()) {
	    case CHANGE: return "change";
	    case DELETE: return "delete";
	    case INSERT: return "insert";
	    default: return "naughtydelta";
	}
    }

    // QName xml_base = new QName("xml", "http://www.w3.org/XML/1998/namespace" ,"base");
    static private final QName c_textdiff = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"textdiff");
    static private final QName c_delta = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"delta");
    static private final QName c_orig = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"original");
    static private final QName c_rev = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"revised");
    static private final QName c_chunk = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"chunk");

    private static void chunkToXML(
	    TreeWriter tree,
	    QName elem,
	    Chunk<String> chunk
    ) throws SaxonApiException {
	// we want to make,
	// <delta type="original">
	//   <chunk position="3" lines="2">
	//     <line>stuff</line>
	//     <line>stuff</line>
	//   </chunk>
	// </delta>
	tree.addStartElement(elem);
	tree.addAttribute(new QName("position"), Integer.toString(chunk.getPosition()));
	tree.addAttribute(new QName("size"), Integer.toString(chunk.size()));

	ListIterator<String> eachLine = chunk.getLines().listIterator();
	while (eachLine.hasNext()) {
	    tree.addStartElement(c_chunk);
	    tree.addText(eachLine.next());
	    tree.addEndElement();
	}
	tree.addEndElement(); // c_rev
    }

    private static XdmNode patchToXML(
	    Patch<String> patch,
	    XProcRuntime runtime,
	    String original_uri,
	    String revised_uri
    ) throws SaxonApiException {
	// Make a simple XML structure
	
	// use xml: for e.g. xml:base

	TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(null); // argument is base URI
        tree.addStartElement(c_textdiff);
	tree.addAttribute(new QName("original-uri"), original_uri);
	tree.addAttribute(new QName("revised-uri"), revised_uri);
	// Add java text utils version as an attribute??

	// I am not sure we need a base attribute
        // tree.addAttribute(xml_base, baseuri.toString());

	for (Delta<String> delta: patch.getDeltas()) {
	    tree.addStartElement(c_delta);
            tree.addAttribute(new QName("type"), deltaType(delta));

	    Chunk<String> orig = delta.getOriginal();
	    if (orig != null) {
		chunkToXML(tree, c_orig, orig);
	    }

	    Chunk<String> rev = delta.getRevised();
	    if (rev != null) {
		chunkToXML(tree, c_rev, rev);
	    }

	    tree.addEndElement(); // c_delta
        }
        tree.addEndElement();
        tree.endDocument();
        return tree.getResult();
    }

    @Override
    public void run() throws SaxonApiException {
	super.run();

	// don't care if we have no input
	//
	// TODO support Base64 input, text input, URI input on each channel
	String original_uri = null;
	String revised_uri = null;

	String s = getOption(_original_uri, (String) null);
	if (s != null) {
	    original_uri = s;
	}

	s = getOption(_revised_uri, (String) null);
	if (s != null) {
	    revised_uri = s;
	}

	try {
	    List<String> original = fileToLines(original_uri);
	    List<String> revised  = fileToLines(revised_uri);

	    // Compute diff. Get the Patch object. Patch is the container for computed deltas.
	    Patch<String> patch = DiffUtils.diff(original, revised);
	    result.write(patchToXML(patch, runtime, original_uri, revised_uri));

	} catch (Exception e) {
	    throw new XProcException(e);
	}
    } /* run */

    public static List<String> fileToLines(String filename) {
	List<String> lines = new LinkedList<String>();
	String line = "";
	BufferedReader in = null;
	try {
	    in = new BufferedReader(new FileReader(filename));
	    while ((line = in.readLine()) != null) {
		lines.add(line);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		    // ignore ... any errors should already have been
		    // reported via an IOException from the final flush.
		}
	    }
	}
	return lines;
    }



    public static void main(String[] args) {

    }

}
