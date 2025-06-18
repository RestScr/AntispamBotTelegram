import React,{useState,useEffect}from "react";
import { FixedSizeList as List } from "react-window";
import { styledComponents as S } from "./style/style";
import {loadLog} from "./config/api";

export default function Log({log}) {

    console.log(log);
    const Row = ({ index, style, data }) => (
        <div style={style}>
            <S.ButtonsInfo>
                <div>
                    <label>{data[index].target}</label>
                    <label>{data[index].reason}</label>
                </div>
                <label>{data[index].data}</label>
            </S.ButtonsInfo>
        </div>
    );

    return (
        <S.Container>
            <h1>Логи</h1>
            <List
                height={600}
                itemCount={log.length}
                itemSize={50}
                width="100%"
                itemData={log}
            >
                {Row}
            </List>
        </S.Container>
    );
}
