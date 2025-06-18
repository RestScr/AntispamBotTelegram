import React,{useState,useEffect} from 'react';
import ReactDOM from 'react-dom/client';
import {useRoutes} from 'react-router-dom';
import ChannelsList from "./ChannelsList";
import Valid from "./Valid";
import InfoChannel from "./InfoChannel";
import reportWebVitals from './reportWebVitals';
import {BrowserRouter} from "react-router";

function Routers() {
    const router = useRoutes([
        { path: '/', element: <Valid /> },
        { path: '/channels', element: <ChannelsList /> },
        { path: '/channels/:channelName', element: <InfoChannel /> },
    ])
    return router;
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <>
        <BrowserRouter>
            <React.StrictMode>
                <Routers />
            </React.StrictMode>
        </BrowserRouter>
    </>
);

reportWebVitals();