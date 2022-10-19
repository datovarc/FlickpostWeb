function doPostCall(){
    tableFilters.length = 0;
    var normalizedFromDate = $('#from_date').val();
    var normalizedToDate = $('#to_date').val();
    var normalizedHub = $('#search_hub').val();
    var normalizedTrackingNo = $('#search-tracking_no').val();

    if (normalizedFromDate !== "") {
        tableFilters.push(JSON.parse('{"field": "dateTime", "type":">=", "value":"'+ normalizedFromDate+ '"}'));
    }
    if (normalizedToDate !== "") {
        tableFilters.push(JSON.parse('{"field": "dateTime", "type":"<=", "value":"'+ normalizedToDate+ '"}'));
    }
    if (normalizedHub !== "") {
        tableFilters.push(JSON.parse('{"field": "hub", "type":"=", "value":"'+ normalizedHub+ '"}'));
    }
    if (normalizedTrackingNo !== "") {
        tableFilters.push(JSON.parse('{"field": "trackingNumber", "type":"=", "value":"'+ normalizedTrackingNo+ '"}'));
    }

    table.setFilter(tableFilters);
    $('.searchloader').hide();
}