package net.canadensys.dataportal.vascan.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.canadensys.dataportal.vascan.ChecklistService;
import net.canadensys.dataportal.vascan.NameService;
import net.canadensys.dataportal.vascan.SearchService;
import net.canadensys.dataportal.vascan.TaxonService;
import net.canadensys.dataportal.vascan.VernacularNameService;
import net.canadensys.dataportal.vascan.model.NameConceptModelIF;
import net.canadensys.dataportal.vascan.model.NameConceptTaxonModel;
import net.canadensys.dataportal.vascan.model.NameConceptVernacularNameModel;
import net.canadensys.dataportal.vascan.model.TaxonModel;
import net.canadensys.dataportal.vascan.model.VernacularNameModel;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * Vascan controller.
 * 
 * @author canadensys
 *
 */
@Controller
public class VascanController {
	
	public static final String JSON_CONTENT_TYPE = "application/json";
	
	@Autowired
	private TaxonService taxonService;
	
	@Autowired
	private VernacularNameService vernacularNameService;
	
	@Autowired
	private NameService nameService;
	
	@Autowired
	private ChecklistService checklistService;
	
	@Autowired
	private SearchService searchService;
	
	@RequestMapping(value={"/taxon/{taxonId}"}, method={RequestMethod.GET})
	public ModelAndView handleTaxon(@PathVariable Integer taxonId){

	    
	    Map<String,Object> model = new HashMap<String,Object>();
	    // attach data to document root
	    model.put("data",taxonService.retrieveTaxonData(taxonId));
	    
	    // add extra data to page global hashmap
//	    _page.put("rank",LabelMappings.getRankLabel(rankid));
//	    _page.put("isSynonym",isSynonymConcept);
	    
		return new ModelAndView("taxon", model);
	}
	
	@RequestMapping(value={"/vernacular/{id}"}, method={RequestMethod.GET})
	public ModelAndView handleVernacular(@PathVariable Integer id){
		
	    VernacularNameModel vernacularName = vernacularNameService.loadVernacularNameModel(id);
	
	    //denormalize data related to this vernacular name
	    Map<String,Object> data = new HashMap<String,Object>();
	    data.put("vernacularId",vernacularName.getId());
	    data.put("name",vernacularName.getName());
	    data.put("reference",vernacularName.getReference().getReference());
	    data.put("referenceShort",vernacularName.getReference().getReferenceshort());
	    data.put("link",vernacularName.getReference().getUrl());
	    data.put("language",vernacularName.getLanguage());
	    data.put("status",vernacularName.getStatus().getStatus());
	    
	    //denormalize data related the related taxon
	    TaxonModel relatedTaxon = vernacularName.getTaxon();
	    HashMap<String,Object> t = new HashMap<String,Object>();
	    t.put("taxonId",relatedTaxon.getId());
	    t.put("fullScientificName",relatedTaxon.getLookup().getCalnamehtmlauthor());
	    t.put("fullScientificNameUrl",relatedTaxon.getLookup().getCalname());
	    t.put("reference",relatedTaxon.getReference().getReference());
	    t.put("referenceShort",relatedTaxon.getReference().getReferenceshort());
	    t.put("link",relatedTaxon.getReference().getUrl());
	    t.put("status",relatedTaxon.getStatus().getStatus());
	    t.put("rank",relatedTaxon.getRank().getRank());
	    data.put("taxon",t);
	    
	    Map<String,Object> model = new HashMap<String,Object>();
	    model.put("vernacularName",data);
	    
	    // add extra data to page global hashmap
	    //_page.put("isVernacular",true);
	    
	    return new ModelAndView("vernacular",model);
	}
	
	@RequestMapping(value={"/name/{name}"}, method={RequestMethod.GET})
	public ModelAndView handleName(@PathVariable String name, @RequestParam(required=false) String redirect){

	    Map<String,Object> model = new HashMap<String,Object>();
	    Map<String,Object> extra = new HashMap<String,Object>();
	    // attach data to document root
	    model.put("data",nameService.retrieveNameData(name, redirect,extra));
	    
	    model.put("extra",extra);
	    
	    // add extra data to page global hashmap
//	    _page.put("rank",LabelMappings.getRankLabel(rankid));
//	    _page.put("isSynonym",isSynonymConcept);
	    
		return new ModelAndView("name", model);
	}
	
	@RequestMapping(value={"/checklist"}, method={RequestMethod.GET})
	public ModelAndView handleChecklist(HttpServletRequest request){
		
		Map<String,Object> model = new HashMap<String,Object>();
		request.getParameterMap();
		
		model.put("data", checklistService.retrieveChecklistData(request.getParameterMap()));
		model.put("pageQuery",StringUtils.defaultString(request.getQueryString()));
		
		return new ModelAndView("checklist", model);
	}
	
	@RequestMapping(value={"/search"}, method={RequestMethod.GET})
	public ModelAndView handleSearch(@RequestParam(required=false) String q){
		Map<String,Object> model = new HashMap<String,Object>();
		
	    HashMap<String,Object> search = new HashMap<String,Object>();
	    search.put("term", "");

	    if(StringUtils.isNotBlank(q)){
		    List<NameConceptModelIF> nameConceptModelList = searchService.searchName(q);
		    
		    Map<String,String> searchRow = null;
		    for(NameConceptModelIF currNameConceptModel : nameConceptModelList){
		    	if(currNameConceptModel.getClass().equals(NameConceptTaxonModel.class)){
		    		searchRow = new HashMap<String, String>();
		    		searchRow.put("type","taxon");
		    		searchRow.put("name", currNameConceptModel.getName());
		    		searchRow.put("id", Integer.toString(currNameConceptModel.getTaxonId()));
		    		searchRow.put("status", currNameConceptModel.getStatus());
		    	}
		    	else if(currNameConceptModel.getClass().equals(NameConceptVernacularNameModel.class)){
		    		searchRow = new HashMap<String, String>();
		    		searchRow.put("type","vernacular");
		    		searchRow.put("name", currNameConceptModel.getName());
		    		searchRow.put("id", Integer.toString(currNameConceptModel.getTaxonId()));
		    		searchRow.put("status", currNameConceptModel.getStatus());
		    		searchRow.put("lang",((NameConceptVernacularNameModel)currNameConceptModel).getLang());
		    	}
		    	else{
		    		//logger
		    	}
		    }
	    }
	    
	    model.put("search",search);
	    
	    return new ModelAndView("search", model);
	}
	
	@RequestMapping(value={"/search.json"}, method={RequestMethod.GET})
	public void handleSearchJson(@RequestParam String q,@RequestParam String t, HttpServletResponse response){
		
		//make sure the answer is set as UTF-8
		response.setCharacterEncoding("UTF-8");
		response.setContentType(JSON_CONTENT_TYPE);
		
		List<NameConceptModelIF> nameConceptModelList = null;
		if("taxon".equalsIgnoreCase(t)){
			nameConceptModelList = searchService.searchTaxon(q);
		}
		else if("vernacular".equalsIgnoreCase(t)){
			nameConceptModelList = searchService.searchVernacularName(q);
		}
		
		JSONArray responseJson = new JSONArray();
		for(NameConceptModelIF currNameConcept : nameConceptModelList){
			responseJson.put(currNameConcept.getName());
		}
		try {
			response.getWriter().print(responseJson.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}