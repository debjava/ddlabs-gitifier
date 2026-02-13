package com.ddlab.rnd.bitbucket.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Data
@NoArgsConstructor
public class BitbucketRepoInput {

	@JsonProperty("scm")
	private String scm = "git";

	@JsonProperty("description")
	private String shortDescription;

	public BitbucketRepoInput(String shortDesciption) {
		this.shortDescription = shortDesciption;
	}

	public String toJson() {
		ObjectMapper mapper = new ObjectMapper();
		String toJson = null;
		try {
			toJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Exception in BitbucketRepoInput.toJson(): \n{}", e);
		}
		return toJson;
	}
}
