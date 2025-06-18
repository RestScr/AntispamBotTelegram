import React, { useState } from "react";
import { Input } from "antd";
import { styledComponents as S } from "./style/style";
import { joinWhiteList } from "./config/api";

export default function JoinWhitelist(id) {
    const [whiteUserName, setWhiteUserName] = useState("");
    const [status, setStatus] = useState(null);

    const handleAddUser = async () => {
        try {
            const res = await joinWhiteList({chatID:id.id,user:whiteUserName});
            const data = await res.json();
            console.log(data);
            if (data.status === true) {
                setStatus(true);
                setWhiteUserName('')
            } else {
                setStatus(false);
            }
        } catch (e) {
            console.error("Ошибка запроса", e);
            setStatus(false);
        }
    };

    return (
        <S.ModalContent>
            <Input
                placeholder="Введите @username"
                value={whiteUserName}
                onChange={(e) => setWhiteUserName(e.target.value)}
            />
            <S.ButtonsInfo>
                <label>
                    {status === true && "Добавлено"}
                    {status === false && "Не удалось добавить"}
                </label>
                <S.Button onClick={handleAddUser}>
                    Добавить
                </S.Button>
            </S.ButtonsInfo>
        </S.ModalContent>
    );
}
