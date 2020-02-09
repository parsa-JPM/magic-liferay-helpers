<%@ include file="/init.jsp" %>

<p>
	<b><liferay-ui:message key="dpcolibrary.caption"/></b>
</p>


<script>

/*
 * it gives all selects that have data-binded-select attr and tryn to load data into defined binded selects
 */
 */
$("select[data-binded-select]").on('change', function() {			
	var selectValue = $(this).val().trim().replace(/ /g, "%20");
	
	var selectIputs = $(this).data("binded-select").split("|");
	var resources = $(this).data("binded-resource").split("|");
	
	for(select  in selectIputs){
		  $(selectIputs[select])
	       .load(resources[select]+"&<portlet:namespace />select_value="+selectValue)
	}
  
  })

</script>