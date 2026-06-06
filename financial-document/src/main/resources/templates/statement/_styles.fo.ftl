<#-- ─── Couleurs ──────────────────────────────────────────── -->
<#assign
  C_NAVY    = "#003366"
  C_BORDER  = "#c8c8c8"
  C_BG_BLUE = "#e8f0fe"
  C_GREY    = "#787878"
  C_DARK    = "#333333"
  C_MID     = "#555555"
  C_DEBIT   = "#b40000"
  C_CREDIT  = "#007800"
  C_ROW_ALT = "#f9f9f9"
  BD        = "0.5pt solid #c8c8c8"
/>

<#-- ─── Macro : ligne info titulaire ───────────────────── -->
<#macro infoRow label value top=false>
          <fo:table-row>
            <fo:table-cell background-color="#f5f5f5"<#if top> border-top="${BD}"</#if> border-bottom="${BD}" padding="5pt 8pt">
              <fo:block font-weight="bold">${label}</fo:block>
            </fo:table-cell>
            <fo:table-cell<#if top> border-top="${BD}"</#if> border-bottom="${BD}" padding="5pt 8pt">
              <fo:block>${value}</fo:block>
            </fo:table-cell>
          </fo:table-row>
</#macro>

<#-- ─── Macro : cellule entête tableau opérations ──────── -->
<#macro opsHeader text>
            <fo:table-cell border="${BD}" padding="5pt" text-align="center">
              <fo:block font-weight="bold">${text}</fo:block>
            </fo:table-cell>
</#macro>