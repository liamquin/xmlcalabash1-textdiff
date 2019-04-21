<p:library xmlns:p="http://www.w3.org/ns/xproc"
	   xmlns:dc="http://www.delightfulcomputing.com/ns/"
           version="1.0">

<p:declare-step type="dc:textdiff">
   <p:input port="original"/>
   <p:input port="revised"/>
   <p:output port="result"/>
   <p:option name="original-uri" />
   <p:option name="original-text" />
   <p:option name="revised-uri" />
   <p:option name="revised-text" />
</p:declare-step>
</p:library>
