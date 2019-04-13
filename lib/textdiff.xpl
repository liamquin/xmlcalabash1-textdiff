<p:library xmlns:p="http://www.w3.org/ns/xproc"
	   xmlns:dc="http://www.delightfulcomputing.com/ns/"
           version="1.0">

<p:declare-step type="dc:textdiff">
   <p:input port="source"/>
   <p:output port="result"/>
   <p:option name="original-uri" />
   <p:option name="revised-uri" />
</p:declare-step>
</p:library>
