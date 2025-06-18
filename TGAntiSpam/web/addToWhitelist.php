<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");
header("Access-Control-Allow-Credentials: true");

include "connect.php";
$status = false;

if (!isset($_REQUEST["chatID"]) || !isset($_REQUEST["user"])) {
    $data = ["error" => "Некорректный запрос"];
} else {
    $chatID = $_REQUEST["chatID"];
    $user = $_REQUEST['user'];
    $stmt = $db->prepare("SELECT * FROM whitelist WHERE chatID = ? AND tg_name = ?");
    
    $stmt->bind_param("ss", $chatID, $user);
    $stmt->execute() or die("Не удалось обработать запрос");
    $result = $stmt->get_result();
    
    $data = [];
    
    if (!($wtData = mysqli_fetch_array($result))) {
    $stmt = $db->prepare("INSERT INTO `whitelist` (`chatID`, `tg_name`, `date`) VALUES (?, ?, ?)");
    
    $stmt->bind_param("sss", $chatID, $user, time());
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
