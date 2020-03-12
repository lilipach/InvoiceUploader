package com.intuit.developer.tutorials.controller;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.intuit.developer.tutorials.helper.RecordsHelper;
import com.intuit.ipp.data.*;
import com.intuit.ipp.data.Error;
import com.monitorjbl.xlsx.StreamingReader;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.developer.tutorials.client.OAuth2PlatformClientFactory;
import com.intuit.developer.tutorials.helper.QBOServiceHelper;
import com.intuit.ipp.core.IEntity;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.exception.InvalidTokenException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;

/**
 * @author dderose
 *
 */
@Controller
public class InvoiceController {


	@Autowired
	OAuth2PlatformClientFactory factory;

	@Autowired
	RecordsHelper recordsHelper;

	@Autowired
	public QBOServiceHelper helper;

	private static final Logger logger = Logger.getLogger(InvoiceController.class);

	private static final String ACCOUNT_QUERY = "select * from Account where AccountType='%s' maxresults 1";


	/**
	 * Sample QBO API call using OAuth2 tokens
	 *
	 * @param session
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/invoice")
	public String populateInvoices(HttpSession session) {

		String realmId = (String) session.getAttribute("realmId");
		if (StringUtils.isEmpty(realmId)) {
			return new JSONObject().put("response", "No realm ID.  QBO calls only work if the accounting scope was passed!").toString();
		}
		String accessToken = (String) session.getAttribute("access_token");

		try {
			//error logger
			File errorFile = new File("ERROR_LOGS.txt");
			FileWriter errorWriter = new FileWriter(errorFile);

			//get DataService
			DataService service = helper.getDataService(realmId, accessToken);

			//Open data set
			InputStream stream = new FileInputStream(new File("C:\\Users\\Liliana\\Documents\\Aclean\\OnlineRetail.xlsx"));
			StreamingReader reader = StreamingReader.builder()
					.rowCacheSize(5)
					.bufferSize(4096)
					.read(stream);

			int count = 0;
			boolean goodRow = true;
			for (Row r : reader) {
				if(checkRow(r) != true)
					goodRow = false;

				for (Cell c : r) {
					System.out.println(c.getStringCellValue() + "\n");
				}
				System.out.println("\n\n\n\n\n\n");

				count++;


				if(count > 6)
					return "Loaded 5 invoices";


				if (count != 1 && goodRow == true){
					try {
						Invoice invoice = fillInvoice(service, r);
						invoice = service.add(invoice);
					}
					catch(FMSException e){
						errorWriter.write("ROW: " + count + "\n" +
											r.getCell(0).getStringCellValue() + "\t" +
										    r.getCell(1).getStringCellValue() + "\t" +
										    r.getCell(2).getStringCellValue() + "\t" +
										    r.getCell(3).getStringCellValue() + "\t" +
											r.getCell(4).getStringCellValue() + "\t" +
											r.getCell(5).getStringCellValue() + "\t" +
											r.getCell(6).getStringCellValue() + "\t" +
											r.getCell(7).getStringCellValue() + "\t");
					}
				}
				goodRow = true;
			}
			return "Invoices uploaded";

		} catch (InvalidTokenException e) {
			return new JSONObject().put("response", "InvalidToken - Refreshtoken and try again").toString();
		} catch (FMSException e) {
			List<Error> list = e.getErrorList();
			list.forEach(error -> logger.error("Error while calling the API :: " + error.getMessage()));

			return new JSONObject().put("response", "Failed").toString();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "hello";
	}

	private boolean checkRow(Row r) {
		int numCells = r.getPhysicalNumberOfCells();
		if(numCells != 8)
			return false;
		/*Note 8th cell is not used so it is not necessary to check its value*/
		for(int i = 0; i < numCells - 1; i++){
			Cell cell = r.getCell(i);
			if((i == 0 || i == 1 || i == 2 || i == 4 || i == 6 ) && cell.getStringCellValue().isEmpty()){
				return false;
			}
			else if(i == 3 || i ==5) {
				if((cell.getCellType() != CellType.NUMERIC) || cell.getStringCellValue().isEmpty()){
					return false;
				}
			}
		}

		return true;
	}

	private Invoice fillInvoice(DataService service, Row r) throws Exception {
		//extract row data
		String stockCode = r.getCell(1).getStringCellValue();
		String description = r.getCell(2).getStringCellValue();
		double quantity = r.getCell(3).getNumericCellValue();
		Date date = r.getCell(4).getDateCellValue();
		double unitPrice = r.getCell(5).getNumericCellValue();
		String customerID = r.getCell(6).getStringCellValue();
		BigDecimal amount = new BigDecimal(quantity * unitPrice);

		//check if customer exists already otherwise create customer
		Customer customer = getCustomerWithAllFields(service, customerID);

		//check if item exists already otherwise create item/service
		Item item = getItemWithAllFields(service, stockCode, description, new BigDecimal(unitPrice));

		//create invoice using customer and item created above
		Invoice invoice = getInvoiceFields(customer, item);
		invoice.getLine().get(0).getSalesItemLineDetail().setQty(new BigDecimal(quantity));
		invoice.getLine().get(0).getSalesItemLineDetail().setUnitPrice(new BigDecimal(unitPrice));
		invoice.getLine().get(0).setDescription(description);
		invoice.getLine().get(0).setAmount(amount);
		invoice.setCustomerRef(createRef(customer));
		invoice.setTxnDate(date);

		return invoice;
	}

		/**
	 * Create Customer request
	 * @return
	 */
	private Customer getCustomerWithAllFields(DataService service, String customerID) throws Exception {
		List<Customer> customers = recordsHelper.getCustomer(service, customerID);
		Customer customer = new Customer();
		if(customers.size() == 0){
			customer = createCustomer(customerID);
			customer = service.add(customer);
		}
		else {
			customer = customers.get(0);
			customer.setActive(Boolean.TRUE);
		}
		return customer;
	}

	private Customer createCustomer(String customerID){
		Customer customer = new Customer();
		customer.setDisplayName(customerID);
		customer.setCompanyName(customerID);

		EmailAddress emailAddr = new EmailAddress();
		emailAddr.setAddress("placeHolder@email.com");
		customer.setPrimaryEmailAddr(emailAddr);

		PhysicalAddress address = new PhysicalAddress();
		address.setCity("randomCity");
		address.setCountry("randomCountry");
		address.setLine1("123 random street");
		address.setPostalCode("12345");

		customer.setBillAddr(address);
		customer.setShipAddr(address);

		customer.setActive(Boolean.TRUE);

		return customer;
	}

	/**
	 * Create Item request
	 * @param service
	 * @return
	 * @throws FMSException
	 */
	private Item getItemWithAllFields(DataService service, String itemStockCode, String itemDes, BigDecimal unitPrice) throws FMSException {
		List<Item> items = recordsHelper.getItem(service, itemStockCode);
		Item item;
		if(items.size() == 0){
			item = createItem(service, itemStockCode, itemDes, unitPrice);
			item = service.add(item);
		}
		else {
			item = items.get(0);
			item.setActive(Boolean.TRUE);
		}
		return item;
	}

	private Item createItem (DataService service, String itemStockCode, String itemDes, BigDecimal unitPrice) throws FMSException {
		Item item = new Item();
		item.setName(itemStockCode);
		item.setDescription(itemDes);
		item.setTaxable(false);
		item.setUnitPrice(unitPrice);
		item.setType(ItemTypeEnum.SERVICE);

		Account incomeAccount = getIncomeBankAccount(service);
		item.setIncomeAccountRef(createRef(incomeAccount));
		item.setActive(Boolean.TRUE);

		return item;
	}

	/**
	 * Get Income account
	 * @param service
	 * @return
	 * @throws FMSException
	 */
	private Account getIncomeBankAccount(DataService service) throws FMSException {
		QueryResult queryResult = service.executeQuery(String.format(ACCOUNT_QUERY, AccountTypeEnum.INCOME.value()));
		List<? extends IEntity> entities = queryResult.getEntities();
		if(!entities.isEmpty()) {
			return (Account)entities.get(0);
		}
		return createIncomeBankAccount(service);
	}

	/**
	 * Create Income account
	 * @param service
	 * @return
	 * @throws FMSException
	 */
	private Account createIncomeBankAccount(DataService service) throws FMSException {
		Account account = new Account();
		account.setName("Incom" + RandomStringUtils.randomAlphabetic(5));
		account.setAccountType(AccountTypeEnum.INCOME);

		return service.add(account);
	}

	/**
	 * Prepare Invoice request
	 * @param customer
	 * @param item
	 * @return
	 */
	private Invoice getInvoiceFields(Customer customer, Item item) {

		Invoice invoice = new Invoice();
		invoice.setCustomerRef(createRef(customer));
		invoice.setBillAddr(customer.getBillAddr());
		invoice.setBillEmail(customer.getPrimaryEmailAddr());

		List<Line> invLine = new ArrayList<Line>();
		Line line = new Line();
		line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);

		SalesItemLineDetail silDetails = new SalesItemLineDetail();
		silDetails.setItemRef(createRef(item));

		line.setSalesItemLineDetail(silDetails);
		invLine.add(line);
		invoice.setLine(invLine);

		return invoice;
	}

	/**
	 * Prepare Payment request
	 * @param
	 * @param invoice
	 * @return
	 */
	private Payment getPaymentFields(ReferenceType customerRef, Invoice invoice) {

		Payment payment = new Payment();
		payment.setCustomerRef(customerRef);

		payment.setTotalAmt(invoice.getTotalAmt());

		List<LinkedTxn> linkedTxnList = new ArrayList<LinkedTxn>();
		LinkedTxn linkedTxn = new LinkedTxn();
		linkedTxn.setTxnId(invoice.getId());
		linkedTxn.setTxnType(TxnTypeEnum.INVOICE.value());
		linkedTxnList.add(linkedTxn);

		Line line1 = new Line();
		line1.setAmount(invoice.getTotalAmt());
		line1.setLinkedTxn(linkedTxnList);

		List<Line> lineList = new ArrayList<Line>();
		lineList.add(line1);
		payment.setLine(lineList);

		return payment;
	}

	/**
	 * Creates reference type for an entity
	 *
	 * @param entity - IntuitEntity object inherited by each entity
	 * @return
	 */
	private ReferenceType createRef(IntuitEntity entity) {
		ReferenceType referenceType = new ReferenceType();
		referenceType.setValue(entity.getId());
		return referenceType;
	}

	/**
	 * Map object to json string
	 * @param entity
	 * @return
	 */
	private String createResponse(Object entity) {
		ObjectMapper mapper = new ObjectMapper();
		String jsonInString;
		try {
			jsonInString = mapper.writeValueAsString(entity);
		} catch (JsonProcessingException e) {
			return createErrorResponse(e);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
		return jsonInString;
	}

	private String createErrorResponse(Exception e) {
		logger.error("Exception while calling QBO ", e);
		return new JSONObject().put("response","Failed").toString();
	}
}