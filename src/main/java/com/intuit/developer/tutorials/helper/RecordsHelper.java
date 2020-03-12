package com.intuit.developer.tutorials.helper;

import com.intuit.ipp.data.*;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecordsHelper {

    @Autowired
    public QBOServiceHelper helper;

    /**Retrieve All Customer Records **/
    public List<Customer> getCustomers(DataService service) throws FMSException {
        List<Customer> customers =  (List<Customer>) service.executeQuery("select * from Customer").getEntities();
        return customers;
    }

    /**Retrieve Specific Customer Record**/
    public List<Customer> getCustomer(DataService service, String customerID) throws Exception {
        List<Customer> customers = (List<Customer>) service.executeQuery("select * from Customer Where DisplayName = " + "'" + customerID + "'").getEntities();
        return customers;
    }

    /**Retrieve All Item Records **/
    public List<Item> getItems(DataService service) throws FMSException {
        List<Item> items =  (List<Item>) service.executeQuery("select * from Item").getEntities();
        return items;
    }

    /**Retrieve Specific Item record **/
    public List<Item> getItem(DataService service, String itemID) throws FMSException {
        List<Item> items =  (List<Item>) service.executeQuery("select * from Item Where Name = " + "'" + itemID + "'").getEntities();
        return items;
    }

    /**Retrieve All Estimate Records **/
    public List<Estimate> getEstimates(DataService service) throws FMSException {
        List<Estimate> estimates =  (List<Estimate>) service.executeQuery("select * from Estimate").getEntities();
        return estimates;
    }

    /**Retrieve All Invoice Records **/
    public  List<Invoice> getInvoices(DataService service) throws Exception {
        List<Invoice> invoices = (List<Invoice>) service.executeQuery("select * from Invoice").getEntities();
        return invoices;
    }

    /**Retrieve All Sales Receipt Records **/
    public List<SalesReceipt> getSalesReceipts(DataService service) throws Exception {
        List<SalesReceipt> salesReceipts = (List<SalesReceipt>) service.executeQuery("select * from SalesReceipt").getEntities();
        return salesReceipts;
    }

    /**Retrieve All Credit Memo Records **/
    public  List<CreditMemo> getCreditMemos(DataService service) throws Exception {
        List<CreditMemo> creditMemos = (List<CreditMemo>) service.executeQuery("select * from CreditMemo").getEntities();
        return creditMemos;
    }

    /**Retrieve All Deposit Records **/
    public  List<Deposit> getDeposits(DataService service) throws Exception {
        List<Deposit> deposits = (List<Deposit>) service.executeQuery("select * from Deposit").getEntities();
        return deposits;
    }

    /**Retrieve All Payment Records **/
    public  List<Payment> getPayments(DataService service) throws Exception {
        List<Payment> payments = (List<Payment>) service.executeQuery("select * from Payment").getEntities();
        return payments;
    }

    /**Retrieve All Bill Records **/
    public  List<Bill> getBills(DataService service) throws Exception {
        List<Bill> bills = (List<Bill>) service.executeQuery("select * from Bill").getEntities();
        return bills;
    }
}
