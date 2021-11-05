
function showContextMenu(){

    let children = document.getElementById("custom-menu").childNodes;

    if(selectedData.length <= 0){
        for(let i=0; i<children.length; i++) {
            children[i].className = "context-option-disabled";
        }
        document.getElementById("exportShipmentsOption").className = "";
    } else{
        for(let i=0; i<children.length; i++) {
            children[i].className = "";
        }
    }

    if(selectedData.length > 1){
        document.getElementById("updateStatusOption").className = "context-option-disabled";
        document.getElementById("addCommentOption").className = "context-option-disabled";
        document.getElementById("editSelectedOption").className = "context-option-disabled";
    }

    let y = window.scrollY + document.querySelector('#checkboxOptionsButton').getBoundingClientRect().top // Y
    let x = window.scrollX + document.querySelector('#checkboxOptionsButton').getBoundingClientRect().left // X

    $(".custom-menu").finish().toggle(100)
        .css({
        top: (y*1.13) + "px",
        left: (x*0.975) + "px"
    });
}


// If the document is clicked somewhere
$(document).bind("mouseup", function (e) {

    // If the clicked element is not the menu
    if (!$(e.target).parents(".custom-menu").length > 0) {

        // Hide it
        $(".custom-menu").hide(100);

    }
});


$(document).ready(function(){

    // If the menu element is clicked
    $(".custom-menu li").click(function(){

        // This is the triggered action name
        switch($(this).attr("data-action")) {

            // A case for each action. Your actions here
            case "updateStatus": alert("Update Status currently disabled."); break;
            case "addComment": alert("Add Comment currently disabled."); break;
            case "exportShipments": alert("Export Shipments currently disabled."); break;
            case "assignHub": alert("Assign Hub currently disabled."); break;
            case "printLabel": alert("Print Label currently disabled."); break;
            case "editSelected":
                document.getElementById('editId').value = selectedData[0].id;
                document.getElementById('editOriginalTN').value = selectedData[0].code;

                document.getElementById('hubEdit').value = selectedData[0].hub;
                document.getElementById('trackingNumberEdit').value = selectedData[0].code;
                document.getElementById('auditedLengthEdit').value = selectedData[0].L;
                document.getElementById('auditedWidthEdit').value = selectedData[0].W;
                document.getElementById('auditedHeightEdit').value = selectedData[0].H;
                document.getElementById('auditedWeightEdit').value = selectedData[0].weight;
                document.getElementById('auditedVolumetricWeightEdit').value = selectedData[0].audited_volumetric_weight;
                document.getElementById('chargeableWeightEdit').value = selectedData[0].chargeable_weight;
                document.getElementById('dateEdit').value = selectedData[0].time.substring(0, 10);
                $("#editLine").modal('toggle');

                break;
            case "deleteSelected":
                if (confirm('Are you sure you want to delete selected package(s) info?')) {
                    deleteSelected();
                }
                break;
        }

        // Hide it AFTER the action was triggered
        $(".custom-menu").hide(100);
    });

});
