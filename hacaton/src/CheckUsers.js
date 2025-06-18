import React from "react";
import { Flex, Spin ,Input} from 'antd';
import { styledComponents as S } from "./style/style";
import { FixedSizeList as List } from "react-window";

export default function CheckUsers() {
    const [users, setUsers] = React.useState([]);
    const [loading, setLoading] = React.useState(false);
    const [buttonActive, setButtonActive] = React.useState(true);
    const [url, setUrl] = React.useState('');

    const handleSubmit = async () => {
        if (!url.trim()) return;
        setLoading(true);
        try {
            const res1 = await fetch("http://127.0.0.1:5001/submit_link", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ link: url })
            });

            const result1 = await res1.json();
            console.log("Результат первого запроса:", result1);
            const res2 = await fetch("http://127.0.0.1:5002/violations", {
                method: "GET"
            });
            const result2 = await res2.json();
            console.log("Результат второго запроса (violations):", result2);

            setUsers(result2);
        } catch (err) {
            console.error(err);
            alert("Что-то пошло не так");
        } finally {
            setLoading(false);
        }
    };

    const Row = ({ index, style, data }) => {
        const user = data[index];
        return (
            <S.CheckContainer style={style}>
                <div>
                    <p>{user.user}</p>
                    <p>{user.reason}</p>
                    <p>{user.date}</p>
                </div>
                <S.Button onClick={() => console.log('ban', user.user)}>Забанить</S.Button>
            </S.CheckContainer>
        );
    };

    return (
        <Flex gap="middle" vertical>
            <Spin spinning={loading} delay={300}>
                <label>
                    Введите ссылку на приглашение в ваш чат — начнётся проверка пользователей:
                </label>
                <S.ButtonsInfo>
                    <Input type="url" value={url} onChange={e => setUrl(e.target.value)} />
                    {buttonActive && (
                        <S.Button onClick={handleSubmit}>Проверить</S.Button>
                    )}
                </S.ButtonsInfo>

                {users.length > 0 && (
                    <List
                        height={1000}
                        itemCount={users.length}
                        itemSize={100}
                        width="100%"
                        itemData={users}
                    >
                        {Row}
                    </List>
                )}
            </Spin>
        </Flex>
    );
}
