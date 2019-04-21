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

    // private static final String library_xpl = "ttps://www.delightfulcomputing.com/xproc/xpl/textdiff.xpl";
    // private static final String library_url = "/com/delightfulcomputing/extensions/textdiff/library.xpl";

    private ReadablePipe originalPort = null;
    private ReadablePipe revisedPort = null;
    private WritablePipe resultPipe = null;

    public TextDiff(XProcRuntime runtime, XAtomicStep step) {
        super(runtime, step);
    }

    public void setInput(String port, ReadablePipe pipe) throws XProcException {
	if (port.equals("original")) {
	    originalPort = pipe;
	} else if (port.equals("revised")) {
	    revisedPort = pipe;
	} else {
	    throw new XProcException(
		"textdiff: unknown port name " + port + " - expected original or revised"
	    );
	}
    }

    public void setOutput(String port, WritablePipe pipe) {
        resultPipe = pipe;
    }

    public void reset() {
        originalPort.resetReader();
        revisedPort.resetReader();
        resultPipe.resetWriter();
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

    private static String getTextFromPort(ReadablePipe source)
	throws SaxonApiException
    {
	XdmNode doc = source.read();
	XdmNode root = S9apiUtils.getDocumentElement(doc);

	final QName _content_type = new QName("content-type");

        if ((XProcConstants.c_data.equals(root.getNodeName())
                && "application/octet-stream".equals(root.getAttributeValue(_content_type)))
                || "base64".equals(root.getAttributeValue(_encoding))) {
            byte[] decoded = Base64.decode(root.getStringValue());
            return new String(decoded);
        } else {
            return root.getStringValue();
        }
    }

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
    } // patchToXML

    @Override
    public void run() throws SaxonApiException {
	super.run();

	/* attributes */
	QName _original_uri = new QName("", "original-uri");
	QName _revised_uri = new QName("", "revised-uri");

	/* or actual content */
	QName _original_text = new QName("", "original-text");
	QName _revised_text = new QName("", "revised-text");

	// don't care if we have no input
	//
	List<String> original = null;
	List<String> revised  = null;

	String original_uri = null;
	String revised_uri = null;

	String s = null;

	// Most of the code in this method is just about trying to find
	// our input.
	// Look for explicitly supplied data first; then look for
	// filenames. That way if both are supplied and the file doesn't
	// exist, you get the error about supplying both, not an I/O
	// exception.
	// Finally, try to read an XProc input port, if there is no text
	// attribute and filename (URI) attribute

	s = getOption(_original_text, (String) null);
	if (s != null) {
	    // [1] got original-text attribute

	    if (getOption(_original_uri, (String) null) != null) {
		throw new XProcException(
			// Do not include the values in the error message as
			// they could be very long, could contain newlines, etc
			"TextDiff: do not supply both original-uri and original-text attributes"
		);
	    }
	    original = new ArrayList<String>(Arrays.asList(
			s.split("\r?[\n\u0085\u2028\u2029]")));
	    original_uri = "data:original-text";
	} else {
	    // original-text arrtibute not given, try for filename
	    s = getOption(_original_uri, (String) null);
	    if (s != null) {
		// [2] got filename
		original_uri = s;
		try {
		    original = fileToLines(original_uri);
		} catch (Exception e) {
		    throw new XProcException(e);
		}
	    } else {
		// nope, no filename, so try input port
		s = getTextFromPort(originalPort);
		if (s != null) {
		    // [3] got input port
		    original = new ArrayList<String>(Arrays.asList(
				s.split("\r?[\n\u0085\u2028\u2029]")));
		    original_uri = "data:original-port";
		} else {
		    // can't find any input
		    throw new XProcException(
			"TextDiff: supply exactly one of original-uri or original-text attributes, or an input <dc:text>...</dc:text> document on port original"
		    );
		}
	    }
	}

	// now the same for revised
	s = getOption(_revised_text, (String) null);
	if (s != null) {
	    // [1] got revised-text attribute

	    if (getOption(_revised_uri, (String) null) != null) {
		throw new XProcException(
			// Do not include the values in the error message as
			// they could be very long, could contain newlines, etc
			"TextDiff: do not supply both revised-uri and revised-text attributes"
		);
	    }
	    revised = new ArrayList<String>(Arrays.asList(
			s.split("\r?[\n\u0085\u2028\u2029]")));
	    revised_uri = "data:revised-text";
	} else {
	    // revised-text arrtibute not given, try for filename
	    s = getOption(_revised_uri, (String) null);
	    if (s != null) {
		// [2] got filename
		revised_uri = s;
		try {
		    revised = fileToLines(revised_uri);
		} catch (Exception e) {
		    throw new XProcException(e);
		}
	    } else {
		// nope, no filename, so try input port
		s = getTextFromPort(revisedPort);
		if (s != null) {
		    // [3] got input port
		    revised = new ArrayList<String>(Arrays.asList(
				s.split("\r?[\n\u0085\u2028\u2029]")));
		    revised_uri = "data:revised-port";
		} else {
		    // can't find any input
		    throw new XProcException(
			"TextDiff: supply exactly one of revised-uri or revised-text attributes, or an input <dc:text>...</dc:text> document on port revised"
		    );
		}
	    }
	}

	// now the easy part (for us!): use the input and make a diff
	try {

	    // Compute diff. Get the Patch object. Patch is the container for computed deltas.
	    Patch<String> patch = DiffUtils.diff(original, revised);
	    resultPipe.write(patchToXML(patch, runtime, original_uri, revised_uri));

	} catch (Exception e) {
	    throw new XProcException(e);
	}
    } // run

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
    } // fileToLines



    public static void main(String[] args) {

    }

}
