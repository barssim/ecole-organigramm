import React, { useCallback } from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
} from '@xyflow/react';

import '@xyflow/react/dist/style.css';

const initialNodes = [
  { id: '1', position: { x: 1050, y: 0 }, data: { label: 'Direction' } },
  { id: '2', position: { x: 2400, y: 160 }, data: { label: 'Secretariat' } },
  { id: '3', position: { x: 450, y: 160 }, data: { label: 'Enseignement' } },
  { id: '4', position: { x: 100, y: 160 }, data: { label: 'Finance' } },
  { id: '11', position: { x: 1050, y: 160 }, data: { label: 'Services' } },

   { id: '12', position: { x: 650, y: 320 }, data: { label: 'Bibleotheque' } },
    { id: '13', position: { x: 850, y: 320 }, data: { label: 'Cantine' } },
     { id: '14', position: { x: 1050, y: 320 }, data: { label: 'Transport' } },
      { id: '15', position: { x: 1250, y: 320 }, data: { label: 'Sport' } },
       { id: '16', position: { x: 1450, y: 320 }, data: { label: 'Menage' } },


  { id: '5', position: { x: 450, y: 320 }, data: { label: 'Classes' } },
  { id: '6', position: { x: 300, y: 480 }, data: { label: 'Matieres' } },
  { id: '7', position: { x: 100, y: 640 }, data: { label: 'Programmes' } },
  { id: '8', position: { x: 300, y: 640 }, data: { label: 'Devoirs' } },
  { id: '9', position: { x: 500, y: 640 }, data: { label: 'Resultas' } },
  { id: '10', position: { x: 650, y: 480 }, data: { label: 'Emploi du temps' } },

  { id: '17', position: { x: 1900, y: 320 }, data: { label: 'Activités' } },
  { id: '18', position: { x: 1700, y: 480 }, data: { label: 'Sorties' } },
  { id: '19', position: { x: 1900, y: 480 }, data: { label: 'Fêtes' } },
  { id: '20', position: { x: 0, y: 320 }, data: { label: 'Payments' } },
  { id: '21', position: { x: 200, y: 320 }, data: { label: 'Factures' } },
  { id: '23', position: { x: 2100, y: 480 }, data: { label: 'Reunions' } },

  { id: '24', position: { x: 2300, y: 320 }, data: { label: 'Inscription' } },
  { id: '25', position: { x: 2500, y: 320 }, data: { label: 'Attestations' } },
  { id: '26', position: { x: 2700, y: 320 }, data: { label: 'Examens' } },
  { id: '27', position: { x: 2900, y: 320 }, data: { label: 'Presence' } },

];

const initialEdges = [
  {
    id: 'e12',
    source: '1',
    target: '2',
    type: 'straight',
    sourcePosition: 'bottom',
    targetPosition: 'top',
  },
  {
      id: 'e1723',
      source: '17',
      target: '23',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
  {
      id: 'e227',
      source: '2',
      target: '27',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
  {
      id: 'e224',
      source: '2',
      target: '24',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
{
    id: 'e225',
    source: '2',
    target: '25',
    type: 'straight',
    sourcePosition: 'bottom',
    targetPosition: 'top',
  },
  {
      id: 'e226',
      source: '2',
      target: '26',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
  {
      id: 'e2223',
      source: '22',
      target: '23',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
  {
      id: 'e420',
      source: '4',
      target: '20',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
{
    id: 'e421',
    source: '4',
    target: '21',
    type: 'straight',
    sourcePosition: 'bottom',
    targetPosition: 'top',
  },
  {
      id: 'e1718',
      source: '17',
      target: '18',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
 {
      id: 'e1719',
      source: '17',
      target: '19',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
  {
      id: 'e217',
      source: '2',
      target: '17',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
   {
      id: 'e111',
      source: '1',
      target: '11',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
    {
            id: 'e1112',
            source: '11',
            target: '12',
            type: 'straight',
            sourcePosition: 'bottom',
            targetPosition: 'top',
          },
       {
                  id: 'e1113',
                  source: '11',
                  target: '13',
                  type: 'straight',
                  sourcePosition: 'bottom',
                  targetPosition: 'top',
                },
   {
         id: 'e1114',
         source: '11',
         target: '14',
         type: 'straight',
         sourcePosition: 'bottom',
         targetPosition: 'top',
       },
   {
            id: 'e1115',
            source: '11',
            target: '15',
            type: 'straight',
            sourcePosition: 'bottom',
            targetPosition: 'top',
          },
   {
            id: 'e1116',
            source: '11',
            target: '16',
            type: 'straight',
            sourcePosition: 'bottom',
            targetPosition: 'top',
          },
  {
        id: 'e67',
        source: '6',
        target: '7',
        type: 'straight',
        sourcePosition: 'bottom',
        targetPosition: 'top',
      },
  {
          id: 'e510',
          source: '5',
          target: '10',
          type: 'straight',
          sourcePosition: 'bottom',
          targetPosition: 'top',
        },
  {
          id: 'e68',
          source: '6',
          target: '8',
          type: 'straight',
          sourcePosition: 'bottom',
          targetPosition: 'top',
        },
    {
            id: 'e69',
            source: '6',
            target: '9',
            type: 'straight',
            sourcePosition: 'bottom',
            targetPosition: 'top',
          },
   {
      id: 'e56',
      source: '5',
      target: '6',
      type: 'straight',
      sourcePosition: 'bottom',
      targetPosition: 'top',
    },
  {
    id: 'e13',
    source: '1',
    target: '3',
    type: 'straight',
    sourcePosition: 'left',
    targetPosition: 'top',
  },
  {
      id: 'e14',
      source: '1',
      target: '4',
      type: 'straight',
      sourcePosition: 'left',
      targetPosition: 'top',
    },
{
      id: 'e35',
      source: '3',
      target: '5',
      type: 'straight',
      sourcePosition: 'left',
      targetPosition: 'top',
    },
];

export default function App() {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  const onConnect = useCallback(
    (params) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const onNodeClick = useCallback((event, node) => {
    if (node.id === '1') {
      alert("Directeur: Il gère toute l'école, il a l'accès à tous les volets.");
    } else if (node.id === '2') {
      alert("Secrétariat: Il se compose de secrétaires et leur chef, le secrétaire général. Ils ont l'accès au volet Administration.");
    } else if (node.id === '3') {
      alert("Enseignement: Il se compose des Professeurs et leur directeur pédagogique");
    }
  }, []);


  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeClick={onNodeClick}
        fitView
      >
        <Controls />
        <MiniMap />
        <Background variant="dots" gap={12} size={1} />
      </ReactFlow>
    </div>
  );
}
