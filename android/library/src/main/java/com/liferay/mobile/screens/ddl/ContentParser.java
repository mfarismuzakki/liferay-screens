package com.liferay.mobile.screens.ddl;

import android.os.Parcel;

import com.liferay.mobile.screens.ddl.model.DDMStructure;
import com.liferay.mobile.screens.ddl.model.Field;
import com.liferay.mobile.screens.util.LiferayLocale;
import com.liferay.mobile.screens.util.LiferayLogger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author Javier Gamarra
 */
public class ContentParser extends XMLParser {

	public List<Field> parseContent(DDMStructure structure, String content) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(content)));

			Element root = document.getDocumentElement();
			return createDocument(root, structure.getFields(), structure.getLocale());
		}
		catch (ParserConfigurationException | IOException | SAXException e) {
			LiferayLogger.e("Error parsing content", e);
		}
		return null;
	}

	public Field getFieldByName(List<Field> fields, String fieldName) {
		if (fieldName == null) {
			return null;
		}

		for (Field f : fields) {
			if (fieldName.equals(f.getName())) {
				return f;
			}
		}

		return null;
	}

	private List<Field> createDocument(Element root, List<Field> currentFields, Locale locale) {

		String tag = "dynamic-element";
		List<Field> fields = new ArrayList<>();
		Locale defaultLocale = LiferayLocale.getDefaultLocale();
		NodeList dynamicElementList = root.getElementsByTagName(tag);

		for (int i = 0; i < dynamicElementList.getLength(); i++) {
			Element dynamicElement = (Element) dynamicElementList.item(i);

			Field field = getFieldByName(currentFields, dynamicElement.getAttribute("name"));
			if (field != null) {
				Element element = getLocaleFallback(dynamicElement, locale, defaultLocale, "dynamic-content", "language-id");
				Field otherField = field.isRepeatable() ? clone(field) : field;
				otherField.setCurrentValue(element.getTextContent());
				if (dynamicElement.getElementsByTagName(tag).getLength() > 0) {
					otherField.setFields(createDocument(dynamicElement, field.getFields(), locale));
				}
				fields.add(otherField);
			}
		}

		return fields;
	}

	private Field clone(Field field) {
		Parcel p = Parcel.obtain();
		p.writeValue(field);
		p.setDataPosition(0);
		Field otherField = (Field) p.readValue(Field.class.getClassLoader());
		p.recycle();
		return otherField;
	}
}
