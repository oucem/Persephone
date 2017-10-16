package re.vianneyfaiv.persephone.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import re.vianneyfaiv.persephone.config.RestTemplateFactory;
import re.vianneyfaiv.persephone.domain.app.Application;
import re.vianneyfaiv.persephone.domain.env.Environment;
import re.vianneyfaiv.persephone.domain.env.PropertyItem;
import re.vianneyfaiv.persephone.exception.ErrorHandler;

/**
 * Calls /env
 */
@Service
public class EnvironmentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

	@Autowired
	private RestTemplateFactory restTemplates;

	public Environment getEnvironment(Application app) {

		String url = app.endpoints().env();

		try {
			LOGGER.debug("GET {}", url);
			ResponseEntity<String> response = restTemplates.get(app).getForEntity(url, String.class);

			String json = response.getBody();

		    ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            JsonNode rootNode = mapper.readTree(json);

            List<PropertyItem> properties = new ArrayList<>();

            Iterator<Entry<String, JsonNode>> iterator = rootNode.fields();

            // Get properties
            while(iterator.hasNext()) {
            	Entry<String, JsonNode> current = iterator.next();

            	current.getValue()
        			.fields()
        			.forEachRemaining(
        				// current.getKey ==> origin of the property : system, application.properties, etc
        				p -> properties.add(new PropertyItem(p.getKey(), p.getValue().asText(), current.getKey()))
					);
            }

			return new Environment(properties);
		}
		catch(RestClientException ex) {
			throw ErrorHandler.handle(app, url, ex);
		}
		catch(IOException e) {
			throw ErrorHandler.handle(app, e);
		}
	}
}
