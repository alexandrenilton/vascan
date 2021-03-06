package net.canadensys.dataportal.vascan.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.canadensys.dataportal.vascan.APIService;
import net.canadensys.dataportal.vascan.config.VascanConfig;
import net.canadensys.dataportal.vascan.constant.Status;
import net.canadensys.dataportal.vascan.dao.NameDAO;
import net.canadensys.dataportal.vascan.dao.TaxonDAO;
import net.canadensys.dataportal.vascan.model.DistributionModel;
import net.canadensys.dataportal.vascan.model.NameConceptModelIF;
import net.canadensys.dataportal.vascan.model.TaxonLookupModel;
import net.canadensys.dataportal.vascan.model.TaxonModel;
import net.canadensys.dataportal.vascan.model.VernacularNameModel;
import net.canadensys.dataportal.vascan.model.api.DistributionAPIResult;
import net.canadensys.dataportal.vascan.model.api.TaxonAPIResult;
import net.canadensys.dataportal.vascan.model.api.TaxonomicAssertionAPIResult;
import net.canadensys.dataportal.vascan.model.api.VascanAPIResponse;
import net.canadensys.dataportal.vascan.model.api.VascanAPIResponseElement;
import net.canadensys.dataportal.vascan.model.api.VernacularNameAPIResult;
import net.canadensys.query.LimitedResult;

import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API Service layer implementation.
 * 
 * @author canadensys
 *
 */
@Service
public class APIServiceImpl implements APIService{
	private static NameParser GBIF_NAME_PARSER = new NameParser();
	
	public static final String API_VERSION = "0.1";
	public static final String ISO3166_2_PREFIX = "ISO 3166-2:";
	
	@Autowired
	private VascanConfig vascanConfig;

	@Autowired
	private NameDAO nameDAO;
	
	@Autowired
	private TaxonDAO taxonDAO;
	
	@Override
	public String getAPIVersion() {
		return API_VERSION;
	}
	
	@Override
	@Transactional(readOnly=true)
	public VascanAPIResponseElement search(String stringValue) {
		return innerSearch(stringValue);
	}
	
	@Override
	@Transactional(readOnly=true)
	public VascanAPIResponse search(String id, String searchTerm) {
		VascanAPIResponse apiResponse = new VascanAPIResponse();
		apiResponse.setApiVersion(API_VERSION);
		apiResponse.setLastUpdatedDate(vascanConfig.getCurrentLastPublicationDate());
		
		VascanAPIResponseElement apiResponseElement = innerSearch(searchTerm);
		apiResponseElement.setLocalIdentifier(id);
		apiResponse.addResult(apiResponseElement);
		return apiResponse;
	}
	
	@Override
	@Transactional(readOnly=true)
	public VascanAPIResponse search(List<String> idList, List<String> dataList) {
		VascanAPIResponse apiResponse = new VascanAPIResponse();
		apiResponse.setApiVersion(API_VERSION);
		apiResponse.setLastUpdatedDate(vascanConfig.getCurrentLastPublicationDate());
		
		int idx=0;
		VascanAPIResponseElement apiResponseElement;
		for(String currId : idList){
			apiResponseElement = innerSearch(dataList.get(idx));
			apiResponseElement.setLocalIdentifier(currId);
			apiResponse.addResult(apiResponseElement);
			idx++;
		}
		return apiResponse;
	}
	
	@Transactional(readOnly=true)
	@Override
	public VascanAPIResponse searchTaxonId(List<Integer> taxonIdList) {
		VascanAPIResponse apiResponse = new VascanAPIResponse();
		apiResponse.setApiVersion(API_VERSION);
		apiResponse.setLastUpdatedDate(vascanConfig.getCurrentLastPublicationDate());

		VascanAPIResponseElement apiResponseElement = null;
		TaxonModel taxonModel = null;
		//we load the TaxonModel for each taxonID (instead of calling taxonDAO.loadTaxonList(...))
		//to easily keep the order and handle missing taxon
		for(Integer taxonId : taxonIdList){
			taxonModel = taxonDAO.loadTaxon(taxonId, false);
			apiResponseElement = new VascanAPIResponseElement();
			apiResponseElement.setSearchedId(taxonId);
			
			if(taxonModel != null){
				apiResponseElement.setNumMatches(1);
				fillVascanAPIResponse(apiResponseElement,Arrays.asList(taxonId),Arrays.asList(1.0f)); //not sure 1.0 is good
			}
			else{
				apiResponseElement.setNumMatches(0);
			}
			apiResponse.addResult(apiResponseElement);
		}
		return apiResponse;
	}
	
	@Transactional(readOnly=true)
	@Override
	public VascanAPIResponse searchTaxonId(Integer id) {
		VascanAPIResponse apiResponse = new VascanAPIResponse();
		apiResponse.setApiVersion(API_VERSION);
		apiResponse.setLastUpdatedDate(vascanConfig.getCurrentLastPublicationDate());
		
		TaxonModel taxonModel = taxonDAO.loadTaxon(id, false);
		
		VascanAPIResponseElement apiResponseElement = new VascanAPIResponseElement();
		
		apiResponseElement.setSearchedId(id);
		
		if(taxonModel != null){
			apiResponseElement.setNumMatches(1);
			fillVascanAPIResponse(apiResponseElement,Arrays.asList(id),Arrays.asList(1.0f)); //not sure 1.0 is good
		}
		else{
			apiResponseElement.setNumMatches(0);
		}
		apiResponse.addResult(apiResponseElement);
		return apiResponse;
	}
	
	/**
	 * Main search function.
	 * Search for a single term.
	 * @param searchTerm
	 * @return VascanAPIResponse object, never null
	 */
	private VascanAPIResponseElement innerSearch(String searchTerm) {
		String pSearchTerm = parseScientificName(searchTerm);
		LimitedResult<List<NameConceptModelIF>> searchResult = nameDAO.search(pSearchTerm,false);
		
		VascanAPIResponseElement apiResponse = new VascanAPIResponseElement();
		apiResponse.setSearchedTerm(searchTerm);
		
		//build a set of unique taxon id
		List<Integer> taxonIdList = new ArrayList<Integer>(searchResult.getRows().size());
		List<Float> scores = new ArrayList<Float>(searchResult.getRows().size());
		for(NameConceptModelIF currName : searchResult.getRows()){
			//avoid duplicated entry (where query matches in taxon name and vernacular name of the same taxon)
			if(!taxonIdList.contains(currName.getTaxonId())){
				taxonIdList.add(currName.getTaxonId());
				//get the score!!
				scores.add(currName.getScore());
			}
		}
		// number of matches excluding duplicates
		apiResponse.setNumMatches(taxonIdList.size());

		if(!taxonIdList.isEmpty()){
			fillVascanAPIResponse(apiResponse,taxonIdList,scores);
		}
		return apiResponse;
	}
	
	/**
	 * Fill the VascanAPIResponse from a list of taxonID.
	 * TODO : optimize function to use arrays instead of lists
	 * @param taxonIdList all taxonID related to a single VascanAPIResponse
	 * @param scores scores associated to taxonId
	 */
	private void fillVascanAPIResponse(VascanAPIResponseElement apiResponse, List<Integer> taxonIdList, List<Float> scores){
		List<TaxonModel> taxonModelList = taxonDAO.loadTaxonList(taxonIdList);
		List<TaxonModel> parentsList;
		
		//use an array to keep TaxonAPIResult in the correct order
		TaxonAPIResult orderedTar[] = new TaxonAPIResult[scores.size()];
		int indexOfTaxon;
		TaxonAPIResult tar;
		TaxonomicAssertionAPIResult taxonomicAssertion;
		
		for(TaxonModel currTaxonModel : taxonModelList){
			parentsList = currTaxonModel.getParents();
			tar = createTaxonAPIResult(currTaxonModel);
			if(currTaxonModel.getStatus().getId() == Status.SYNONYM){
				//synonyms can have more than one parent so we loop over parents
				//end add a new entry for each of the parents
				for(TaxonModel currParent : parentsList){
					taxonomicAssertion = createTaxonomicAssertionAPIResult(currTaxonModel);
					taxonomicAssertion.setAcceptedNameUsageID(currParent.getId());
					taxonomicAssertion.setAcceptedNameUsage(currParent.getLookup().getCalnameauthor());
					tar.addTaxonomicAssertion(taxonomicAssertion);
				}
			}
			else{ //accepted taxon
				taxonomicAssertion = createTaxonomicAssertionAPIResult(currTaxonModel);
				//this will be false for the root only
				if(parentsList.size() == 1){
					taxonomicAssertion.setParentNameUsageID(parentsList.get(0).getId());
				}
				taxonomicAssertion.setHigherClassification(currTaxonModel.getLookup().getHigherclassification());
				taxonomicAssertion.setAcceptedNameUsageID(currTaxonModel.getId());
				taxonomicAssertion.setAcceptedNameUsage(currTaxonModel.getLookup().getCalnameauthor());
				tar.addTaxonomicAssertion(taxonomicAssertion);
			}
			//keep the ordering of taxon
			indexOfTaxon = taxonIdList.indexOf(currTaxonModel.getId());
			//tar.setScore(scores.get(indexOfTaxon));
			orderedTar[indexOfTaxon] = tar;
		}
		//add them all to the apiResponse in the correct order.
		for(TaxonAPIResult currTar : orderedTar){
			apiResponse.addMatch(currTar);
		}
	}
	
	/**
	 * Creates a TaxonomicAssertionAPIResult and fill data that is common to accepted and synonym taxon.
	 * @param taxonModel
	 * @return newly created TaxonomicAssertionAPIResult
	 */
	private TaxonomicAssertionAPIResult createTaxonomicAssertionAPIResult(TaxonModel taxonModel){
		TaxonomicAssertionAPIResult taxonomicAssertion = new TaxonomicAssertionAPIResult();
		TaxonLookupModel tlm = taxonModel.getLookup();
		
		taxonomicAssertion.setTaxonomicStatus(tlm.getStatus());
		taxonomicAssertion.setNameAccordingTo(taxonModel.getReference().getReference());
		taxonomicAssertion.setNameAccordingToID(taxonModel.getReference().getUrl());
		return taxonomicAssertion;
	}
	
	/**
	 * Creates a TaxonAPIResult and fill data that is common to accepted and synonym taxon.
	 * @param taxonModel
	 * @return newly created TaxonAPIResult
	 */
	private TaxonAPIResult createTaxonAPIResult(TaxonModel taxonModel){
		TaxonLookupModel tlm = taxonModel.getLookup();
		
		TaxonAPIResult tar = new TaxonAPIResult();
		tar.setTaxonID(taxonModel.getId());
		
		tar.setCanonicalName(tlm.getCalname());
		tar.setScientificName(tlm.getCalnameauthor());
		tar.setScientificNameAuthorship(tlm.getAuthor());
		tar.setTaxonRank(tlm.getRank());

		for(VernacularNameModel currVernacular : taxonModel.getVernacularnames()){
			VernacularNameAPIResult vnar = new VernacularNameAPIResult();
			vnar.setVernacularName(currVernacular.getName());
			vnar.setLanguage(currVernacular.getLanguage());
			vnar.setSource(currVernacular.getReference().getReference());
			vnar.setPreferredName(currVernacular.getStatus().getId() == Status.ACCEPTED);
			tar.addVernacularName(vnar);
		}
		String iso3166_2Code;
		DistributionAPIResult dar = null;
		for(DistributionModel currDistribution : taxonModel.getDistribution()){
			dar = new DistributionAPIResult();
			dar.setLocality(currDistribution.getRegion().getRegion());
			iso3166_2Code = currDistribution.getRegion().getIso3166_2();
			//avoid null as location id
			dar.setLocationID(StringUtils.defaultString(iso3166_2Code));
			if(StringUtils.isNotBlank(iso3166_2Code)){
				dar.setLocationID(ISO3166_2_PREFIX+iso3166_2Code);
			}
			dar.setOccurrenceStatus(currDistribution.getDistributionStatus().getDistributionstatus());
			dar.setEstablishmentMeans(currDistribution.getDistributionStatus().getEstablishmentmeans());
			tar.addDistribution(dar);
		}
		return tar;
	}
	
	/**
	 * Try to parse the provided name with GBIF name parser. We want to remove the authorship and date if provided.
	 * @param rawScientificName
	 * @return parsed scientific name or the name provided if we can't parse it.
	 */
	private static String parseScientificName(String rawScientificName){
		ParsedName parsedName = null;
		try{
			parsedName = GBIF_NAME_PARSER.parse(rawScientificName);
			if (NameType.WELLFORMED.equals(parsedName.getType())
					|| NameType.SCINAME.equals(parsedName.getType())) {
				return parsedName.canonicalNameWithMarker();
			}
		}
		catch(UnparsableException uEx){} //ignore
		return rawScientificName;
	}
}
