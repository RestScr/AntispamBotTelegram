import React, { useState } from "react";
import { Input } from "antd";
import { styledComponents as S } from "./style/style";
import { deleteUser } from "./config/api";

export default function DeleteWhiteList(id) {
    const [userName, setUserName] = useState("");
    const [status, setStatus] = useState(null);

    const handleDelete = async () => {
        try {
            const res = await deleteUser({chatID:id.id, user:userName});
            const data = await res.json();
            console.log(data);

            if (data.status === true) {
                setStatus(true);
                setUserName("");
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
                value={userName}
                onChange={e => setUserName(e.target.value)}
            />
            <S.ButtonsInfo>
                <label>
                    {status === true && "Удалено"}
                    {status === false && "Ошибка удаления"}
                </label>
                <S.Button onClick={handleDelete}>
                    Удалить
                </S.Button>
            </S.ButtonsInfo>
        </S.ModalContent>
    );
}
