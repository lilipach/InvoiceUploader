package com.intuit.developer.tutorials.controller;

import com.intuit.developer.tutorials.client.OAuth2PlatformClientFactory;
import com.intuit.developer.tutorials.helper.QBOServiceHelper;
import com.intuit.developer.tutorials.helper.RecordsHelper;
import com.intuit.ipp.data.*;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

@Controller
public class ClearRecordsController {

    @Autowired
    OAuth2PlatformClientFactory factory;

    @Autowired
    public QBOServiceHelper helper;

    @Autowired
    public RecordsHelper recordHelper;

    @ResponseBody
    @RequestMapping("/records")
    public String deleteRecords(HttpSession session) {

        String realmId = (String)session.getAttribute("realmId");
        if (StringUtils.isEmpty(realmId)) {
            return new JSONObject().put("response","No realm ID.  QBO calls only work if the accounting scope was passed!").toString();
        }
        String accessToken = (String)session.getAttribute("access_token");
        try {

            DataService service = helper.getDataService(realmId, accessToken);

            deleteRecords(service);
            deleteItems(service);
            deleteCustomers(service);

        }
        catch (Exception e) {

            return "Error: some records could not be deleted.";
        }
        return "All Records Cleared From Account.";
    }

    private void deleteRecords(DataService service) throws Exception {
        List<Deposit> deposits = recordHelper.getDeposits(service);
        List<Estimate> estimates = recordHelper.getEstimates(service);
        List<Invoice> invoices = recordHelper.getInvoices(service);
        List<CreditMemo> memos = recordHelper.getCreditMemos(service);
        List<SalesReceipt> receipts = recordHelper.getSalesReceipts(service);
        List<Payment> payments = recordHelper.getPayments(service);

        for(Deposit d: deposits){
            try{
                service.delete(d);
            }
            catch(Exception exc){
                System.out.println("Record NOT Deleted." + exc);
            }
        }

        for(Estimate e: estimates){
            try{
                service.delete(e);
            }
            catch(Exception exc){
                System.out.println("Record NOT Deleted." + exc.toString());
            }
        }

        //error logger
        File errorFile = new File("ERROR_DELLOGS.txt");
        FileWriter errorWriter = new FileWriter(errorFile);
        errorWriter.append("test: ???\n" +  invoices.size());
        for(Invoice i: invoices){
            try{
                service.delete(i);
                errorWriter.write("record?");
            }
            catch(Exception exc){
                errorWriter.append("Record NOT Deleted.");

            }
        }
        errorWriter.close();
        for(CreditMemo c: memos){
            try{
                service.delete(c);
            }
            catch(Exception exc){
                System.out.println("Record NOT Deleted." + exc);
            }
        }
        for(SalesReceipt s: receipts){
            try{
                service.delete(s);
            }
            catch(Exception exc){
                System.out.println("Record NOT Deleted." + exc);
            }
        }

        for(Payment p: payments){
            try{
                service.delete(p);
            }
            catch(Exception exc){
                System.out.println("Record NOT Deleted." + exc);
            }
        }

        return;
    }

    private void deleteItems(DataService service) throws FMSException {
        System.out.println("Items prob \n\n\n");
        List<Item> items = recordHelper.getItems(service);

        for(Item i: items){
            try {
                if (!i.getFullyQualifiedName().equals("Hours") && !i.getFullyQualifiedName().equals("Services"))
                    i.setActive(Boolean.FALSE);
                service.update(i);
            } catch(Exception e){
                System.out.println("Record NOT deleted");
            }

        }
    }

    /** Note This funtion does not work when deleting Customer Sub Customers */
    private void deleteCustomers(DataService service) throws FMSException {
        List<Customer> customers = recordHelper.getCustomers(service);

        for(Customer c: customers){
            try {
                c.setActive(Boolean.FALSE);
                service.update(c);
            }
            catch(Exception e){
                System.out.println("Record NOT Deleted.");
            }
        }
    }
}
