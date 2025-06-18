<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");
header("Access-Control-Allow-Credentials: true");

include "connect.php";
$status = false;

if (!isset($_REQUEST["chatID"]) || !isset($_REQUEST["filter"])) {
    $data = ["error" => "Некорректный запрос"];
} else {
    $chatID = $_REQUEST["chatID"];
    $filter = $_REQUEST['filter'];
    $stmt = $db->prepare("SELECT * FROM filters WHERE chatID = ? AND type = ?");
    
    $stmt->bind_param("ss", $chatID, $filter);
    $stmt->execute() or die("Не удалось обработать запрос");
    $result = $stmt->get_result();
    
    $data = [];
    
    if (!($wtData = mysqli_fetch_array($result))) {
    $stmt = $db->prepare("INSERT INTO `filters` (`chatID`, `type`, `data`) VALUES (?, ?, ?)");
    
    $dataa = "-";
    $stmt->bind_param("sss", $chatID, $filter, $dataa);
    $stmt->execute() or die("Не удалось обработать запрос");
       $status = true;
    }
}

$answer = [
    "status" => $status,
    "data" => $data,
];

echo json_encode($answer, JSON_UNESCAPED_UNICODE);
?>
