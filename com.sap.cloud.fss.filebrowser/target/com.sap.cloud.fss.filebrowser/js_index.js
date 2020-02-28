var rootPathDirectory = "/usr/sap/ljs/FSS";
var treeURL = "/webapi/tree";
var treeURLDownloadFile = "/webapi/tree/download";
var treeURLDownloadZipFile = "/webapi/tree/zipdownload";
var treeURLDelete = "/webapi/tree/delete";
var stackOfPaths = new Array();
var currentPath = new Array();
var flag;

$(document).ready(function() {
	  sendPostRequest(rootPathDirectory);
});

function dataDisplay(pathDirectories) {
	var responseText = pathDirectories["directory"];
	pageCount = pathDirectories["size"];
	flag = false;
	  
	var column = [];
	for (var i = 0; i < responseText.length; i++) {
		for (var key in responseText[i]) {
			if (column.indexOf(key) === -1 && key != "path" && key != "isFolder" && key != "size") {
				column.push(key);
			}
		}
	}

	var table = document.createElement("table");
	table.setAttribute("id", "myTable");
	
	var tableHeader = document.createElement("thead");
	table.appendChild(tableHeader);
	
	var tableRow = table.insertRow(-1);

	for (var i = 0; i < column.length; i++) {
		var tableHead = document.createElement("th");
		tableHead.innerHTML = column[i];
		tableHeader.appendChild(tableHead);
	}

	for (var i = 0; i < responseText.length; i++) {
		tableRow = table.insertRow(-1);

		for (var j = 0; j < column.length; j++) {
			var tabCell = tableRow.insertCell(-1);
			if (responseText[i]["isFolder"] === true) {
				tabCell.innerHTML = "<input type=checkbox value ='" + responseText[i]["path"] + "'> <img class=folder ' onClick='sendPathDirectoryRequest(\"" + responseText[i]["path"] + "\")'> <div class='" + responseText[i]["path"] + "'type='" + responseText[i]["isFolder"]
				+ "' onClick='sendPathDirectoryRequest(\"" + responseText[i]["path"] + "\")'>" + responseText[i][column[j]] + "</div>";			
			} else {
				tabCell.innerHTML = "<input type=checkbox class=file value ='" + responseText[i]["path"] + "'> <label for=submit-form> <img for=submit-form class=anyfile ' onClick='sendDownloadFileRequest(\""
					+ responseText[i]["path"] + "\")'> </label> <div> <label for=submit-form class='" + responseText[i]["path"] + "'type='" + responseText[i]["isFolder"] + "' onClick='sendDownloadFileRequest(\"" + responseText[i]["path"] + "\")'>" + responseText[i][column[j]] + "</label> </div>";					
			}
		}
	}
	var divContainer = document.getElementById("showData");
	divContainer.innerHTML = "";
	divContainer.appendChild(table);  
	
	pagination(pageCount);
	flag = true;
}

function pagination(pageCount) {
	var items = $("table tbody tr td");
	var perPage = 10;
    items.slice(perPage).hide();
    
	$('#pageNumbers').pagination({
		items: pageCount,
	    itemsOnPage: perPage,
	    cssStyle: "light-theme",
	    });
	
	function checkFragment() {	
		 var hash = window.location.hash || "#page-1";
	     hash = hash.match(/^#page-(\d+)$/);
	     
	     if(hash) {
	    	 $('#pageNumbers').pagination("selectPage", parseInt(hash[1]));
	    	 
	      	if (flag === true) {
	         sendPostRequest(currentPath.pop());
	         flag = false;
	         }
		}
	}
	
	$(window).bind("popstate", checkFragment); 
	checkFragment();
}

function submitCheckboxRequest() {
	var arrayPaths = new Array();
	$("input:checked").each(function() {
		var path = $(this).attr("value");
		arrayPaths.push(path);
	});

	if (arrayPaths === undefined || arrayPaths.length == 0) {
		throw new Error("You can't download !!! Choose a file or folder to download");
	} else {
		var button = document.getElementById("your-id");
		button.value = arrayPaths.toString();
	}
}

function submitDeleteCheckboxRequest() {
	var arrayPaths = new Array();
	$("input:checked").each(function () {
	      var path = $(this).attr("value");	  	
	      var filePath = validateDeleteAndUploadPath(path);
	      arrayPaths.push(filePath);
	      });	
	
	var path = arrayPaths.toString();
	var currentPath = validateFilePath(path);
	
	var currentURL = window.location.href;
	var pageNumber;
	if (currentURL.indexOf('#page') > -1) {
		pageNumber = currentURL.substring(currentURL.indexOf("-") + 1);
	} else {
		pageNumber = 1;
	}
	
	$.ajax({
	type: "POST",
        url: treeURLDelete,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        data: JSON.stringify({path: path, pageNumber}),
        cache: false,
        context: this,
        success: function(currentPath){
        	 dataDisplay(currentPath);
		},
		failure: function(errMsg) {
			alert("Your file is not deleted !");
		}
	});	
}

if (performance.navigation.type == 1) {
	var cur = window.location.href.slice(0, -1);
	window.location.href = cur + "1";
	} else {
		console.info("This page is not reloaded");
		}

function sendPathDirectoryRequest(path){	
	stackOfPaths.push(path);
	
	sendPostRequest(path);
	
	selectPathBreadcrumbs(path);
}

function sendPostRequest(path){
	currentPath.push(path);
	var currentURL = window.location.href;
	var pageNumber;
	if (currentURL.indexOf('#page') > -1) {
		pageNumber = currentURL.substring(currentURL.indexOf("-") + 1);
	} else {
		pageNumber = 1;
	}
	
	$.ajax({
	type: "POST",
        url: treeURL,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        data: JSON.stringify({path: path, pageNumber}),
        cache: false,
        context: this,
        success: function(path){
            dataDisplay(path);
	},
	failure: function(errMsg){
		alert(errMsg);
		}
	});
}
    
function sendDownloadFileRequest(previousPath){
	var input = document.getElementById("submit-form");
	var path = validateFilePath(previousPath);
	input.value = path;
}

document.getElementById('upload').onchange = uploadOnChange;
function uploadOnChange() {
    var filename = this.value;
    document.getElementById('filename').value = filename;
}


function goBack() {
	if (isNaN(stackOfPaths)) {
		var currentPath = stackOfPaths.pop();
		var previousPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
	    
	    var path = validateFilePath(previousPath);
	    
		selectPathBreadcrumbs(path);
		sendPostRequest(path);
	}
}
	
function validateFilePath(path) {	
    if (path.indexOf('FSS') > -1 ) {
    	return path;
    } else {
    	throw new Error("You are in the root directory");
    }
}

function validateDeleteAndUploadPath(path) {	
	var subPath = path.substring('/usr/sap/ljs/FSS'.length + 1);
	
	var volumeName;
	if (subPath.indexOf('/') > -1) {
		volumeName = subPath.substring(0, subPath.indexOf('/'));
	} else {
	    volumeName = subPath;
	}
	
	if ((subPath.indexOf(volumeName) > -1) && (subPath.length > volumeName.length)) {
		return path;
	} else {
		throw new Error("Path directory is not correct");
	}
}

function selectPathBreadcrumbs(currentDirectory) {
	$( ".direcktory" ).remove();
	$("#pathInput").val(currentDirectory);
	
	var directories = currentDirectory.split('/');
	for( var i = 0; i < directories.length; i++ ) {
	    var part = directories[i];
	    var text = part.toUpperCase();
	    var links = directories.slice( 0, i + 1 ).join('/');
	    var link = links.substring(0, links.length); 
	    if (link.indexOf('FSS') > -1) {
			$('#breadcrumb ol.breadcrumbs').append("<li class=direcktory path='" + link + "'>" + text + "</li>");
	    }
	}
}

$("#breadcrumb ol.breadcrumbs").on( "click", "li", function() {
	   var path = $(this).attr("path");
	   sendPathDirectoryRequest(path);
	});