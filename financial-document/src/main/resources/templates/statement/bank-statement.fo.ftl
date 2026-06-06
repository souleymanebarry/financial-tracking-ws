<?xml version="1.0" encoding="UTF-8"?>
<#include "_styles.fo.ftl">
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <fo:layout-master-set>
    <fo:simple-page-master master-name="A4"
        page-width="210mm" page-height="297mm"
        margin-top="0mm" margin-bottom="15mm"
        margin-left="15mm" margin-right="15mm">
      <fo:region-body margin-top="0mm" margin-bottom="10mm"/>
      <fo:region-after extent="8mm"/>
    </fo:simple-page-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="A4">

    <fo:static-content flow-name="xsl-region-after">
      <fo:block text-align="center" font-size="7pt" color="${C_GREY}" font-family="Helvetica">
        Page <fo:page-number/>
      </fo:block>
    </fo:static-content>

    <fo:flow flow-name="xsl-region-body"
        font-family="Helvetica" font-size="9pt" color="${C_DARK}">

      <!-- En-tête : bande d'accent latérale + fond clair -->
      <fo:block-container border-left="6pt solid ${C_NAVY}" background-color="#fafafa"
          padding="10pt 14pt" margin-bottom="12pt">
        <fo:table table-layout="fixed" width="100%">
          <fo:table-column column-width="55%"/>
          <fo:table-column column-width="45%"/>
          <fo:table-body>
            <fo:table-row>
              <fo:table-cell display-align="center">
                <fo:block font-size="18pt" font-weight="bold" color="${C_NAVY}">
                  ${labels["statement.title"]}
                </fo:block>
              </fo:table-cell>
              <fo:table-cell text-align="right" display-align="center">
                <fo:block font-size="8pt" color="${C_MID}">${labels["statement.period"]}</fo:block>
                <fo:block font-size="11pt" font-weight="bold" color="${C_DARK}">
                  ${periodStart} &#8211; ${periodEnd}
                </fo:block>
              </fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>
      </fo:block-container>

      <!-- Informations titulaire -->
      <fo:table table-layout="fixed" width="100%" margin-bottom="12pt">
        <fo:table-column column-width="25%"/>
        <fo:table-column column-width="75%"/>
        <fo:table-body>
          <@infoRow label=labels["statement.holder"]       value=holderName  top=true/>
          <@infoRow label=labels["statement.email"]        value=email/>
          <@infoRow label=labels["statement.rib"]          value=rib/>
          <@infoRow label=labels["statement.generated.at"] value=generatedAt/>
        </fo:table-body>
      </fo:table>

      <!-- Résumé des soldes : deux cartes séparées avec accent top -->
      <fo:table table-layout="fixed" width="100%" margin-bottom="12pt">
        <fo:table-column column-width="48.5%"/>
        <fo:table-column column-width="3%"/>
        <fo:table-column column-width="48.5%"/>
        <fo:table-body>
          <fo:table-row>
            <fo:table-cell background-color="${C_BG_BLUE}"
                border-top="3pt solid ${C_NAVY}" border-left="${BD}" border-right="${BD}" border-bottom="${BD}"
                padding="12pt" text-align="center">
              <fo:block font-size="8pt" color="${C_MID}" margin-bottom="4pt">
                ${labels["statement.opening.balance"]}
              </fo:block>
              <fo:block font-size="16pt" font-weight="bold" color="${C_NAVY}">
                ${openingBalance}
              </fo:block>
            </fo:table-cell>
            <fo:table-cell><fo:block/></fo:table-cell>
            <fo:table-cell background-color="${C_BG_BLUE}"
                border-top="3pt solid ${C_NAVY}" border-left="${BD}" border-right="${BD}" border-bottom="${BD}"
                padding="12pt" text-align="center">
              <fo:block font-size="8pt" color="${C_MID}" margin-bottom="4pt">
                ${labels["statement.closing.balance"]}
              </fo:block>
              <fo:block font-size="16pt" font-weight="bold" color="${C_NAVY}">
                ${closingBalance}
              </fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>

      <!-- Opérations -->
      <fo:block font-size="11pt" font-weight="bold" margin-bottom="4pt">
        ${labels["statement.operations.title"]}
      </fo:block>

      <fo:table table-layout="fixed" width="100%" font-size="8pt">
        <fo:table-column column-width="14%"/>
        <fo:table-column column-width="18%"/>
        <fo:table-column column-width="43%"/>
        <fo:table-column column-width="13%"/>
        <fo:table-column column-width="12%"/>
        <fo:table-header>
          <fo:table-row background-color="${C_BG_BLUE}">
            <@opsHeader text=labels["statement.col.date"]/>
            <@opsHeader text=labels["statement.col.number"]/>
            <@opsHeader text=labels["statement.col.label"]/>
            <@opsHeader text=labels["statement.col.amount"]/>
            <@opsHeader text=labels["statement.col.balance"]/>
          </fo:table-row>
        </fo:table-header>
        <fo:table-body>
          <#if lines?has_content>
            <#list lines as line>
              <fo:table-row background-color="${(line?index % 2 == 0)?then('#ffffff', C_ROW_ALT)}">
                <fo:table-cell border="${BD}" padding="4pt" text-align="center">
                  <fo:block>${line.date}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="${BD}" padding="4pt">
                  <fo:block>${line.number}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="${BD}" padding="4pt">
                  <fo:block>${line.label}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="${BD}" padding="4pt" text-align="right">
                  <#if line.isDebit>
                    <fo:block color="${C_DEBIT}">- ${line.amount}</fo:block>
                  <#else>
                    <fo:block color="${C_CREDIT}">+ ${line.amount}</fo:block>
                  </#if>
                </fo:table-cell>
                <fo:table-cell border="${BD}" padding="4pt" text-align="right">
                  <fo:block>${line.runningBalance}</fo:block>
                </fo:table-cell>
              </fo:table-row>
            </#list>
          <#else>
            <fo:table-row>
              <fo:table-cell number-columns-spanned="5" border="${BD}" padding="8pt" text-align="center">
                <fo:block color="${C_GREY}">${labels["statement.no.operations"]}</fo:block>
              </fo:table-cell>
            </fo:table-row>
          </#if>
        </fo:table-body>
      </fo:table>

      <!-- Pied légal -->
      <fo:block font-size="7pt" color="${C_GREY}" margin-top="20pt"
          border-top="0.5pt solid #e0e0e0" padding-top="6pt">
        ${labels["statement.legal.footer"]}
      </fo:block>

    </fo:flow>
  </fo:page-sequence>

</fo:root>
