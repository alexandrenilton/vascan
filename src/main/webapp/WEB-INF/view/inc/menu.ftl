<#assign fullScreenPages = ["checklist", "darwincore", "dwc", "search", "api", "about"]>

<div id="body"<#if fullScreenPages?seq_contains(currentPage)> class="fullscreen body_${currentPage}"</#if>>
<#if currentPage == "search">
<!--[if lte IE 7]>
<p id="ie">${rc.getMessage("browser_msg1")}</p>
<![endif]-->
</#if>
<div id="side_bar">
	<ul class="side_menu custom_list round">
		<li><a href="${rc.getContextUrl("/search"+rc.getMessage("url_language"))}">${rc.getMessage("namesearch_title1")}</a></li>
		<li><a href="${rc.getContextUrl("/checklist"+rc.getMessage("url_language"))}">${rc.getMessage("checklist_title1")}</a></li>
		<li><a href="${rc.getContextUrl("/about"+rc.getMessage("url_language"))}">${rc.getMessage("about_title1")}</a></li>
		<li><a href="${rc.getContextUrl("/api"+rc.getMessage("url_language"))}">${rc.getMessage("api_title1")}</a></li>
		<li><a href="${rc.getMessage("url_dataset")}">${rc.getMessage("dataset_title1")}</a></li>
	</ul>

<#if currentPage == "name" && page.extra.isDisambiguation != true>
<#--Link to taxon page-->
	<#if data.isRedirect != true && page.extra.isSynonym == true>
		<p><a class="round big_button multi_line" id="see_taxon" href="${rc.getContextUrl("/taxon/"+data.synonymWarningId+rc.getMessage("url_language"))}">${rc.getMessage("page_button4")}</a></p>
	<#elseif data.isRedirect != true && page.extra.isVernacular == true>
		<p><a class="round big_button multi_line" id="see_taxon" href="${rc.getContextUrl("/vernacular/"+data.vernacularNameWarningId+rc.getMessage("url_language"))}">${rc.getMessage("page_button5")}</a></p>
	<#else>
		<p><a class="round big_button multi_line" id="see_taxon" href="${rc.getContextUrl("/taxon/"+data.id+rc.getMessage("url_language"))}">${rc.getMessage("page_button4")}</a></p>
	</#if>
<#elseif currentPage == "taxon" && data.status == "accepted">
		<p><a class="round big_button multi_line" id="create_checklist" href="${getI18nContextUrl("/checklist?taxon="+data.taxonId)}">${rc.getMessage("page_button1")}</a></p>
<#elseif currentPage == "checklist">
	<#if (data.isSearch)?? && data.isSearch>
		<p><a class="round big_button multi_line" id="dwc_archive" href="${rc.getContextUrl("/download?format=dwc&"+page.pageQuery)}">${rc.getMessage("page_button2")}</a></p>
		<p><a class="round big_button multi_line" id="csv_file" href="${rc.getContextUrl("/download?format=txt&"+page.pageQuery)}">${rc.getMessage("page_button3")}</a></p>
	</#if>
</#if>
<#if currentPage == "name" || currentPage == "taxon" || currentPage == "vernacular" || currentPage="checklist">
		<p><a class="round big_button" id="print_page" href="javascript:window.print();">${rc.getMessage("page_button6")}</a></p>
</#if>
</div>
		
<div id="content" class="clear_fix">