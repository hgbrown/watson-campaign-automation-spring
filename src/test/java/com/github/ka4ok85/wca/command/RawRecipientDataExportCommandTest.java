package com.github.ka4ok85.wca.command;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import com.github.ka4ok85.wca.config.SpringConfig;
import com.github.ka4ok85.wca.constants.FileEncoding;
import com.github.ka4ok85.wca.constants.Visibility;
import com.github.ka4ok85.wca.options.RawRecipientDataExportOptions;
import com.github.ka4ok85.wca.response.JobResponse;
import com.github.ka4ok85.wca.response.RawRecipientDataExportResponse;
import com.github.ka4ok85.wca.response.ResponseContainer;
import com.github.ka4ok85.wca.response.containers.JobPollingContainer;
import com.github.ka4ok85.wca.sftp.SFTP;
import com.github.ka4ok85.wca.utils.DateTimeRange;

@RunWith(value = Parameterized.class)
@ContextConfiguration(classes = { SpringConfig.class })
public class RawRecipientDataExportCommandTest {

	@ClassRule
	public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	private String xmlNodeName;
	private String methodName;

	@Autowired
	ApplicationContext context;

	private String defaultRequest = String.join(System.getProperty("line.separator"),
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", "<Envelope>", "<Body>",
			"<RawRecipientDataExport>", "<EXPORT_FORMAT>CSV</EXPORT_FORMAT>", "<FILE_ENCODING>utf-8</FILE_ENCODING>",
			"<MOVE_TO_FTP/>", "<SENT_MAILINGS/>", "<ALL_EVENT_TYPES/>", "</RawRecipientDataExport>", "</Body>",
			"</Envelope>");

	public RawRecipientDataExportCommandTest(String xmlNodeName, String methodName) {
		this.xmlNodeName = xmlNodeName;
		this.methodName = methodName;
	}

	@Test(expected = NullPointerException.class)
	public void testBuildXmlDoesNotAcceptNullOptions() {
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = null;
		command.buildXmlRequest(options);
	}

	@Test
	public void testBuildXmlDefaultRequest() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest;
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsDoNotIncludeChildren() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		Long listId = 3L;
		options.setListId(listId);
		options.setIncludeChildren(false);
		options.setMoveToFtp(false);
		options.setIncludeSentMailings(false);

		List<String> columns = new ArrayList<String>();
		options.setColumns(columns);

		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest
				.replace("<RawRecipientDataExport>", "<RawRecipientDataExport><LIST_ID>" + listId + "</LIST_ID>")
				.replace("<MOVE_TO_FTP/>", "").replace("<SENT_MAILINGS/>", "");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsMailingReportId() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		List<HashMap<String, Long>> mailingReportId = new ArrayList<HashMap<String, Long>>();
		HashMap<String, Long> map = new HashMap<String, Long>();
		map.put("mailingId", 5L);
		map.put("reportId", 6L);
		mailingReportId.add(map);

		options.setMailingReportId(mailingReportId);
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("<RawRecipientDataExport>",
				"<RawRecipientDataExport><MAILING><MAILING_ID>5</MAILING_ID><REPORT_ID>6</REPORT_ID></MAILING>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsMailingReportWithoutKeys() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		List<HashMap<String, Long>> mailingReportId = new ArrayList<HashMap<String, Long>>();
		HashMap<String, Long> map = new HashMap<String, Long>();
		mailingReportId.add(map);
		options.setMailingReportId(mailingReportId);

		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("<RawRecipientDataExport>",
				"<RawRecipientDataExport><MAILING></MAILING>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsCampaignId() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		Long campaignId = 2L;
		options.setCampaignId(campaignId);
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("<RawRecipientDataExport>",
				"<RawRecipientDataExport><CAMPAIGN_ID>" + campaignId + "</CAMPAIGN_ID>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsListId() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		Long listId = 3L;
		options.setListId(listId);
		options.setIncludeChildren(true);
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("<RawRecipientDataExport>",
				"<RawRecipientDataExport><LIST_ID>" + listId + "</LIST_ID><INCLUDE_CHILDREN></INCLUDE_CHILDREN>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsEventRange() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		LocalDateTime startDateTime = LocalDateTime.of(2017, 2, 3, 4, 5);
		LocalDateTime endDateTime = LocalDateTime.of(2018, 6, 7, 8, 9);
		DateTimeRange eventRange = new DateTimeRange(startDateTime, endDateTime);
		options.setEventRange(eventRange);
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
		String eventRangeString = "<EVENT_DATE_START>" + startDateTime.format(formatter)
				+ "</EVENT_DATE_START><EVENT_DATE_END>" + endDateTime.format(formatter) + "</EVENT_DATE_END>";
		String controlString = defaultRequest.replace("<RawRecipientDataExport>",
				"<RawRecipientDataExport>" + eventRangeString);
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsSendRange() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		LocalDateTime startDateTime = LocalDateTime.of(2017, 2, 3, 4, 5);
		LocalDateTime endDateTime = LocalDateTime.of(2018, 6, 7, 8, 9);
		DateTimeRange sendRange = new DateTimeRange(startDateTime, endDateTime);
		options.setSendRange(sendRange);
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
		String eventRangeString = "<SEND_DATE_START>" + startDateTime.format(formatter)
				+ "</SEND_DATE_START><SEND_DATE_END>" + endDateTime.format(formatter) + "</SEND_DATE_END>";
		String controlString = defaultRequest.replace("<RawRecipientDataExport>",
				"<RawRecipientDataExport>" + eventRangeString);
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsVisibilityPrivate() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		options.setVisibility(Visibility.PRIVATE);

		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("<MOVE_TO_FTP/>", "<MOVE_TO_FTP/><PRIVATE>TRUE</PRIVATE>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsVisibilityShared() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		options.setVisibility(Visibility.SHARED);

		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("<MOVE_TO_FTP/>", "<MOVE_TO_FTP/><SHARED>TRUE</SHARED>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsAllNonExported() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		options.setAllNonExported(true);
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("<RawRecipientDataExport>",
				"<RawRecipientDataExport><ALL_NON_EXPORTED></ALL_NON_EXPORTED>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsReturnFromAddress() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		options.setReturnFromAddress(true);
		;
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("</EXPORT_FORMAT>",
				"</EXPORT_FORMAT><RETURN_FROM_ADDRESS></RETURN_FROM_ADDRESS>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsReturnFromName() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		options.setReturnFromName(true);
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("</EXPORT_FORMAT>",
				"</EXPORT_FORMAT><RETURN_FROM_NAME></RETURN_FROM_NAME>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testBuildXmlHonorsExportFileName() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		String exportFileName = "test file.zip";
		options.setExportFileName(exportFileName);
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String controlString = defaultRequest.replace("</FILE_ENCODING>",
				"</FILE_ENCODING><EXPORT_FILE_NAME>" + exportFileName + "</EXPORT_FILE_NAME>");
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Parameterized.Parameters(name = "{index}: isValid({0})={1}")
	public static Iterable<Object[]> data() {
		return Arrays
				.asList(new Object[][] { { "<SENDING></SENDING>", "setIncludeSendingMailings" },
						{ "<OPTIN_CONFIRMATION></OPTIN_CONFIRMATION>", "setIncludeOptinConfirmationMailings" },
						{ "<PROFILE_CONFIRMATION></PROFILE_CONFIRMATION>", "setIncludeProfileConfirmationMailings" },
						{ "<AUTOMATED></AUTOMATED>", "setIncludeAutomatedMailings" },
						{ "<CAMPAIGN_ACTIVE></CAMPAIGN_ACTIVE>", "setIncludeCampaignActiveMailings" },
						{ "<CAMPAIGN_COMPLETED></CAMPAIGN_COMPLETED>", "setIncludeCampaignCompletedMailings" },
						{ "<CAMPAIGN_CANCELLED></CAMPAIGN_CANCELLED>", "setIncludeCampaignCancelledMailings" },
						{ "<CAMPAIGN_SCRAPE_TEMPLATE></CAMPAIGN_SCRAPE_TEMPLATE>",
								"setIncludeCampaignScrapeTemplateMailings" },
						{ "<INCLUDE_TEST_MAILINGS></INCLUDE_TEST_MAILINGS>", "setIncludeTestMailings" } });
	}

	@Test
	public void testIsValidMailingParameters() {
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		java.lang.reflect.Method method;
		try {
			method = options.getClass().getMethod(methodName, boolean.class);
			method.invoke(options, true);

			command.buildXmlRequest(options);
			String testString = command.getXML();
			Source test = Input.fromString(testString).build();

			// get control XML
			String controlString = defaultRequest.replace("<SENT_MAILINGS/>", "<SENT_MAILINGS/>" + xmlNodeName);
			Source control = Input.fromString(controlString).build();

			Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
			Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());

		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testBuildXmlHonorsExportColumns() {
		// get XML from command
		RawRecipientDataExportCommand command = new RawRecipientDataExportCommand();
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		List<String> columns = new ArrayList<String>();
		columns.add("Column A");
		columns.add("Column B");
		options.setColumns(columns);
		command.buildXmlRequest(options);
		String testString = command.getXML();
		Source test = Input.fromString(testString).build();

		// get control XML
		String columnsString = "<COLUMNS>";
		for (String column : columns) {
			columnsString = columnsString + "<COLUMN><NAME>" + column + "</NAME></COLUMN>";
		}

		columnsString = columnsString + "</COLUMNS>";
		String controlString = defaultRequest.replace("<ALL_EVENT_TYPES/>", "<ALL_EVENT_TYPES/>" + columnsString);
		Source control = Input.fromString(controlString).build();

		Diff myDiff = DiffBuilder.compare(control).withTest(test).ignoreWhitespace().checkForSimilar().build();
		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

	@Test
	public void testReadResponse() {
		RawRecipientDataExportCommand command = context.getBean(RawRecipientDataExportCommand.class);
		SFTP sftp = mock(SFTP.class);
		command.setSftp(sftp);
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		String localAbsoluteFilePath = "/local/path/data.csv";
		options.setLocalAbsoluteFilePath(localAbsoluteFilePath);
		Long jobId = 34L;
		JobPollingContainer jobPollingContainer = new JobPollingContainer();
		jobPollingContainer.setJobId(jobId);
		Map<String, String> parameters = new HashMap<String, String>();
		String filePath = "/path/to/file.csv";
		parameters.put("FILE_PATH", filePath);
		jobPollingContainer.setParameters(parameters);

		String jobDescription = "String Job Description";
		String eventTypes = "Test List";
		Long exportedRowCount = 55L;
		String mailingTypes = "";

		Integer timeZone = 4;
		String fileEncoding = "iso-8859-1";
		JobResponse jobResponse = new JobResponse();
		jobResponse.setJobDescription(jobDescription);
		Map<String, String> jobParameters = new HashMap<String, String>();
		jobParameters.put("EVENT_TYPES", eventTypes);
		jobParameters.put("FILE_ENCODING", fileEncoding);
		jobParameters.put("EXPORTED_ROW_COUNT", exportedRowCount.toString());
		jobParameters.put("MAILING_TYPE_ARRAY", mailingTypes);
		jobParameters.put("TIME_ZONE", timeZone.toString());
		jobResponse.setParameters(jobParameters);

		ResponseContainer<RawRecipientDataExportResponse> responseContainer = command.readResponse(jobPollingContainer,
				jobResponse, options);
		RawRecipientDataExportResponse response = responseContainer.getResposne();

		assertEquals(response.getDescription(), jobDescription);
		assertEquals(response.getFileEncoding(), FileEncoding.ISO_8859_1);
		assertEquals(response.getEventTypes(), eventTypes);
		assertEquals(response.getExportedRowCount(), exportedRowCount);
		assertEquals(response.getMailingTypes(), mailingTypes);
		assertEquals(response.getJobId(), jobId);
		assertEquals(response.getTimeZone(), timeZone);
		assertEquals(response.getRemoteFileName(), filePath);
		verify(sftp, times(1)).download(filePath, localAbsoluteFilePath);
	}

	@Test
	public void testReadResponseHonorsNoMoveToFtp() {
		RawRecipientDataExportCommand command = context.getBean(RawRecipientDataExportCommand.class);
		SFTP sftp = mock(SFTP.class);
		command.setSftp(sftp);
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		String localAbsoluteFilePath = "/local/path/data.csv";
		options.setLocalAbsoluteFilePath(localAbsoluteFilePath);
		options.setMoveToFtp(false);
		Long jobId = 34L;
		JobPollingContainer jobPollingContainer = new JobPollingContainer();
		jobPollingContainer.setJobId(jobId);
		Map<String, String> parameters = new HashMap<String, String>();
		String filePath = "/path/to/file.csv";
		parameters.put("FILE_PATH", filePath);
		jobPollingContainer.setParameters(parameters);

		String jobDescription = "String Job Description";
		String eventTypes = "Test List";
		Long exportedRowCount = 55L;
		String mailingTypes = "";

		Integer timeZone = 4;
		String fileEncoding = "iso-8859-1";
		JobResponse jobResponse = new JobResponse();
		jobResponse.setJobDescription(jobDescription);
		Map<String, String> jobParameters = new HashMap<String, String>();
		jobParameters.put("EVENT_TYPES", eventTypes);
		jobParameters.put("FILE_ENCODING", fileEncoding);
		jobParameters.put("EXPORTED_ROW_COUNT", exportedRowCount.toString());
		jobParameters.put("MAILING_TYPE_ARRAY", mailingTypes);
		jobParameters.put("TIME_ZONE", timeZone.toString());
		jobResponse.setParameters(jobParameters);

		ResponseContainer<RawRecipientDataExportResponse> responseContainer = command.readResponse(jobPollingContainer,
				jobResponse, options);
		RawRecipientDataExportResponse response = responseContainer.getResposne();

		assertEquals(response.getDescription(), jobDescription);
		assertEquals(response.getFileEncoding(), FileEncoding.ISO_8859_1);
		assertEquals(response.getEventTypes(), eventTypes);
		assertEquals(response.getExportedRowCount(), exportedRowCount);
		assertEquals(response.getMailingTypes(), mailingTypes);
		assertEquals(response.getJobId(), jobId);
		assertEquals(response.getTimeZone(), timeZone);
		assertEquals(response.getRemoteFileName(), filePath);
		verify(sftp, times(0)).download(filePath, localAbsoluteFilePath);
	}
	
	@Test
	public void testReadResponseHonorsBlankLocalAbsoluteFilePath() {
		RawRecipientDataExportCommand command = context.getBean(RawRecipientDataExportCommand.class);
		SFTP sftp = mock(SFTP.class);
		command.setSftp(sftp);
		RawRecipientDataExportOptions options = new RawRecipientDataExportOptions();
		String localAbsoluteFilePath = "/local/path/data.csv";
		Long jobId = 34L;
		JobPollingContainer jobPollingContainer = new JobPollingContainer();
		jobPollingContainer.setJobId(jobId);
		Map<String, String> parameters = new HashMap<String, String>();
		String filePath = "/path/to/file.csv";
		parameters.put("FILE_PATH", filePath);
		jobPollingContainer.setParameters(parameters);

		String jobDescription = "String Job Description";
		String eventTypes = "Test List";
		Long exportedRowCount = 55L;
		String mailingTypes = "";

		Integer timeZone = 4;
		String fileEncoding = "iso-8859-1";
		JobResponse jobResponse = new JobResponse();
		jobResponse.setJobDescription(jobDescription);
		Map<String, String> jobParameters = new HashMap<String, String>();
		jobParameters.put("EVENT_TYPES", eventTypes);
		jobParameters.put("FILE_ENCODING", fileEncoding);
		jobParameters.put("EXPORTED_ROW_COUNT", exportedRowCount.toString());
		jobParameters.put("MAILING_TYPE_ARRAY", mailingTypes);
		jobParameters.put("TIME_ZONE", timeZone.toString());
		jobResponse.setParameters(jobParameters);

		ResponseContainer<RawRecipientDataExportResponse> responseContainer = command.readResponse(jobPollingContainer,
				jobResponse, options);
		RawRecipientDataExportResponse response = responseContainer.getResposne();

		assertEquals(response.getDescription(), jobDescription);
		assertEquals(response.getFileEncoding(), FileEncoding.ISO_8859_1);
		assertEquals(response.getEventTypes(), eventTypes);
		assertEquals(response.getExportedRowCount(), exportedRowCount);
		assertEquals(response.getMailingTypes(), mailingTypes);
		assertEquals(response.getJobId(), jobId);
		assertEquals(response.getTimeZone(), timeZone);
		assertEquals(response.getRemoteFileName(), filePath);
		verify(sftp, times(0)).download(filePath, localAbsoluteFilePath);
	}
}
