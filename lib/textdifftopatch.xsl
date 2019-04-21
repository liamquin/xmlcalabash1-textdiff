<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:dc="http://www.delightfulcomputing.com/ns/"
  exclude-result-prefixes="dc"
  >

  <!--* Convert XML from the TextDiff XProc step into
      * something like Unix diff output.
      *-->

  <xsl:output method="text"/>

  <xsl:template match="dc:textdiff">
    <xsl:apply-templates select="dc:delta"/>
  </xsl:template>

  <xsl:template match="dc:delta[@type = 'change']">
    <!--* first the lines affected *-->
    <xsl:value-of select="dc:original/@position + 1"/>
    <xsl:if test="dc:original/@size &gt; 1">
      <xsl:text>,</xsl:text>
      <xsl:value-of select="dc:original/@size + 1"/>
    </xsl:if>
    <xsl:text>c</xsl:text>
    <xsl:value-of select="dc:revised/@position + 1"/>
    <xsl:if test="dc:revised/@size &gt; 1">
      <xsl:text>,</xsl:text>
      <xsl:value-of select="dc:revised/@size"/>
    </xsl:if>
    <xsl:text>&#xa;</xsl:text>

    <xsl:apply-templates select="dc:original"/>

    <xsl:text>---&#xa;</xsl:text> <!--* separator *-->

    <xsl:apply-templates select="dc:revised"/>
  </xsl:template>

  <xsl:template match="dc:original">
    <xsl:for-each select="dc:chunk">
      <xsl:value-of select="concat('&lt; ', ., '&#xa;')"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="dc:revised">
    <xsl:for-each select="dc:chunk">
      <xsl:value-of select="concat('&gt; ', ., '&#xa;')"/>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
